package sorbonne.professional_website.jobs;

import java.time.OffsetDateTime;
import sorbonne.professional_website.time.PlatformTime;

public record BackgroundJobResponse(
        String id, Long ownerId, Long versionId, BackgroundJobType type, BackgroundJobStatus status,
        int progress, int priority, int attempts, int maxAttempts, OffsetDateTime executeAfter, OffsetDateTime startedAt,
        OffsetDateTime heartbeatAt, OffsetDateTime completedAt, String lastError, String correlationId,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static BackgroundJobResponse from(BackgroundJob job) {
        return new BackgroundJobResponse(job.getId(), job.getOwnerId(), job.getVersionId(), job.getType(), job.getStatus(),
                job.getProgress(), job.getPriority(), job.getAttempts(), job.getMaxAttempts(),
                PlatformTime.asUtcOffset(job.getExecuteAfter()), PlatformTime.asUtcOffset(job.getStartedAt()),
                PlatformTime.asUtcOffset(job.getHeartbeatAt()), PlatformTime.asUtcOffset(job.getCompletedAt()),
                job.getLastError(), job.getCorrelationId(), PlatformTime.asUtcOffset(job.getCreatedAt()), PlatformTime.asUtcOffset(job.getUpdatedAt()));
    }
}
