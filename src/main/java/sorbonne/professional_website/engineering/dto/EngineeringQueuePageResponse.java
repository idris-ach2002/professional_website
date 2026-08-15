package sorbonne.professional_website.engineering.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EngineeringQueuePageResponse(
        String kind,
        int page,
        int size,
        long totalElements,
        int totalPages,
        Integer capacity,
        Integer queued,
        Double saturationPercent,
        List<QueueItem> items
) {
    public record QueueItem(
            String id,
            String type,
            String status,
            Integer progress,
            Integer priority,
            Integer attempts,
            Integer maxAttempts,
            OffsetDateTime createdAt,
            OffsetDateTime scheduledAt,
            OffsetDateTime completedAt
    ) { }
}
