package sorbonne.professional_website.publication;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sorbonne.professional_website.jobs.BackgroundJobService;

import java.time.LocalDateTime;

@Component
public class PublicationJobRunner {
    private final BackgroundJobService jobs;
    private final PublicationService publicationService;

    public PublicationJobRunner(BackgroundJobService jobs, PublicationService publicationService) {
        this.jobs = jobs;
        this.publicationService = publicationService;
    }

    @Scheduled(fixedDelayString = "${app.publication.scheduler-delay-ms:1000}")
    public void executeDuePublications() {
        for (var recovered : jobs.recoverStaleRunningJobs(sorbonne.professional_website.time.PlatformTime.utcNow().minusMinutes(2))) {
            if (recovered.status() == sorbonne.professional_website.jobs.BackgroundJobStatus.FAILED
                    && recovered.type() == sorbonne.professional_website.jobs.BackgroundJobType.PUBLICATION
                    && recovered.versionId() != null) {
                publicationService.markScheduledPublicationFailed(
                        recovered.ownerId(), recovered.versionId(), recovered.correlationId(),
                        new IllegalStateException(recovered.lastError()));
            }
        }
        for (var candidate : jobs.duePublicationJobs(sorbonne.professional_website.time.PlatformTime.utcNow())) {
            var claimed = jobs.claimForExecution(candidate.getId());
            if (claimed.isEmpty()) continue;
            var running = claimed.get();
            try {
                jobs.updateProgress(running.getId(), 30);
                boolean published = publicationService.publishScheduled(running.getOwnerId(), running.getVersionId());
                if (!published) {
                    jobs.markCancelledAfterClaim(running.getId(), "Publication job became stale because the version lifecycle changed.");
                    continue;
                }
                jobs.updateProgress(running.getId(), 85);
                jobs.markSucceeded(running.getId());
            } catch (RuntimeException error) {
                var status = jobs.markFailedOrRetry(running.getId(), error);
                if (status == sorbonne.professional_website.jobs.BackgroundJobStatus.FAILED) {
                    publicationService.markScheduledPublicationFailed(
                            running.getOwnerId(), running.getVersionId(), running.getCorrelationId(), error);
                }
            }
        }
    }
}
