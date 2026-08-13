package sorbonne.professional_website.jobs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.events.OutboxService;
import sorbonne.professional_website.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BackgroundJobService {
    private final BackgroundJobRepository repository;
    private final OutboxService outbox;

    public BackgroundJobService(BackgroundJobRepository repository, OutboxService outbox) { this.repository = repository; this.outbox = outbox; }

    @Transactional
    public BackgroundJob create(Long ownerId, Long versionId, BackgroundJobType type, LocalDateTime executeAfter, String correlationId) {
        int priority = type == BackgroundJobType.PUBLICATION ? 100 : 50;
        BackgroundJob job = repository.save(BackgroundJob.builder().ownerId(ownerId).versionId(versionId).type(type)
                .status(BackgroundJobStatus.QUEUED).progress(0).priority(priority).attempts(0).maxAttempts(3)
                .executeAfter(executeAfter).correlationId(correlationId).build());
        emit(job, "BACKGROUND_JOB_QUEUED");
        return job;
    }

    @Transactional(readOnly = true)
    public List<BackgroundJobResponse> list(Long ownerId) {
        return repository.findTop100ByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(BackgroundJobResponse::from).toList();
    }

    @Transactional
    public BackgroundJobResponse cancel(Long ownerId, String jobId) {
        BackgroundJob job = lock(ownerId, jobId);
        if (job.getStatus() == BackgroundJobStatus.SUCCEEDED || job.getStatus() == BackgroundJobStatus.CANCELLED) return BackgroundJobResponse.from(job);
        if (job.getStatus() == BackgroundJobStatus.RUNNING) throw new IllegalStateException("Running jobs cannot be cancelled once execution has started.");
        job.setStatus(BackgroundJobStatus.CANCELLED); job.setCompletedAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        BackgroundJob saved = repository.save(job); emit(saved, "BACKGROUND_JOB_CANCELLED");
        return BackgroundJobResponse.from(saved);
    }

    @Transactional
    public BackgroundJobResponse retry(Long ownerId, String jobId) {
        BackgroundJob job = lock(ownerId, jobId);
        if (job.getStatus() != BackgroundJobStatus.FAILED) throw new IllegalStateException("Only failed jobs can be retried.");
        // A manual operator retry starts a fresh retry budget. Automatic retries still honor maxAttempts.
        job.setAttempts(0);
        job.setStatus(BackgroundJobStatus.RETRYING); job.setLastError(null); job.setExecuteAfter(sorbonne.professional_website.time.PlatformTime.utcNow()); job.setCompletedAt(null);
        BackgroundJob saved = repository.save(job); emit(saved, "BACKGROUND_JOB_RETRY_QUEUED");
        return BackgroundJobResponse.from(saved);
    }

    @Transactional
    public void cancelPublicationJobs(Long ownerId, Long versionId) {
        var statuses = List.of(BackgroundJobStatus.QUEUED, BackgroundJobStatus.RETRYING);
        for (BackgroundJob job : repository.findByOwnerIdAndVersionIdAndTypeAndStatusIn(ownerId, versionId, BackgroundJobType.PUBLICATION, statuses)) {
            job.setStatus(BackgroundJobStatus.CANCELLED); job.setCompletedAt(sorbonne.professional_website.time.PlatformTime.utcNow()); emit(job, "BACKGROUND_JOB_CANCELLED");
        }
    }

    @Transactional(readOnly = true)
    public List<BackgroundJob> duePublicationJobs(LocalDateTime now) {
        return repository.findTop50ByTypeAndStatusInAndExecuteAfterLessThanEqualOrderByPriorityDescExecuteAfterAsc(
                BackgroundJobType.PUBLICATION, List.of(BackgroundJobStatus.QUEUED, BackgroundJobStatus.RETRYING), now);
    }

    @Transactional
    public java.util.Optional<BackgroundJob> claimForExecution(String id) {
        BackgroundJob job = repository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("BackgroundJob"));
        if (job.getStatus() != BackgroundJobStatus.QUEUED && job.getStatus() != BackgroundJobStatus.RETRYING) {
            return java.util.Optional.empty();
        }
        job.setStatus(BackgroundJobStatus.RUNNING);
        job.setAttempts(job.getAttempts() + 1);
        job.setProgress(10);
        job.setStartedAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        job.setHeartbeatAt(job.getStartedAt());
        BackgroundJob saved = repository.save(job);
        emit(saved, "BACKGROUND_JOB_STARTED");
        return java.util.Optional.of(saved);
    }

    @Transactional
    public void markSucceeded(String id) {
        repository.findById(id).ifPresent(job -> {
            job.setStatus(BackgroundJobStatus.SUCCEEDED); job.setProgress(100); job.setHeartbeatAt(sorbonne.professional_website.time.PlatformTime.utcNow());
            job.setCompletedAt(sorbonne.professional_website.time.PlatformTime.utcNow()); job.setLastError(null); emit(job, "BACKGROUND_JOB_SUCCEEDED");
        });
    }

    @Transactional
    public void markCancelledAfterClaim(String id, String reason) {
        BackgroundJob job = repository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("BackgroundJob"));
        if (job.getStatus() != BackgroundJobStatus.RUNNING) return;
        job.setStatus(BackgroundJobStatus.CANCELLED);
        job.setCompletedAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        job.setHeartbeatAt(job.getCompletedAt());
        job.setLastError(reason);
        emit(job, "BACKGROUND_JOB_CANCELLED_STALE");
    }

    @Transactional
    public BackgroundJobStatus markFailedOrRetry(String id, Throwable error) {
        BackgroundJob job = repository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("BackgroundJob"));
        job.setHeartbeatAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        job.setLastError(error == null ? "Unknown background job failure" : String.valueOf(error.getMessage()));
        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(BackgroundJobStatus.FAILED);
            job.setCompletedAt(sorbonne.professional_website.time.PlatformTime.utcNow());
            emit(job, "BACKGROUND_JOB_FAILED");
        } else {
            job.setStatus(BackgroundJobStatus.RETRYING);
            long backoffSeconds = Math.min(60, 1L << Math.min(job.getAttempts(), 5));
            job.setExecuteAfter(sorbonne.professional_website.time.PlatformTime.utcNow().plusSeconds(backoffSeconds));
            emit(job, "BACKGROUND_JOB_RETRY_QUEUED");
        }
        return job.getStatus();
    }


    @Transactional
    public void updateProgress(String id, int progress) {
        repository.lockById(id).ifPresent(job -> {
            if (job.getStatus() != BackgroundJobStatus.RUNNING) return;
            job.setProgress(Math.max(job.getProgress(), Math.max(0, Math.min(99, progress))));
            job.setHeartbeatAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        });
    }

    @Transactional
    public List<BackgroundJobResponse> recoverStaleRunningJobs(LocalDateTime cutoff) {
        List<BackgroundJob> stale = repository.lockStaleRunning(BackgroundJobStatus.RUNNING, cutoff);
        LocalDateTime now = sorbonne.professional_website.time.PlatformTime.utcNow();
        for (BackgroundJob job : stale) {
            job.setLastError("Recovered after interrupted background execution.");
            job.setHeartbeatAt(now);
            if (job.getAttempts() >= job.getMaxAttempts()) {
                job.setStatus(BackgroundJobStatus.FAILED);
                job.setCompletedAt(now);
                emit(job, "BACKGROUND_JOB_FAILED");
            } else {
                job.setStatus(BackgroundJobStatus.RETRYING);
                job.setExecuteAfter(now);
                job.setCompletedAt(null);
                emit(job, "BACKGROUND_JOB_RECOVERED");
            }
        }
        return stale.stream().map(BackgroundJobResponse::from).toList();
    }

    private BackgroundJob lock(Long ownerId, String id) {
        return repository.lockById(id)
                .filter(job -> job.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("BackgroundJob"));
    }

    private void emit(BackgroundJob job, String type) {
        String key = type + ":" + job.getId() + ":" + job.getAttempts() + ":" + job.getStatus();
        outbox.record(key, job.getOwnerId(), "BackgroundJob", job.getId(), type, Map.of(
                "jobId", job.getId(), "jobType", job.getType().name(), "status", job.getStatus().name(), "progress", job.getProgress()));
    }
}
