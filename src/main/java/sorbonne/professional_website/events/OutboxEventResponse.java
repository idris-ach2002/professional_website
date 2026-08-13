package sorbonne.professional_website.events;

import java.time.OffsetDateTime;
import sorbonne.professional_website.time.PlatformTime;

public record OutboxEventResponse(
        String id, String eventKey, Long ownerId, String aggregateType, String aggregateId, String eventType,
        String payloadJson, OutboxStatus status, int attempts, String lastError,
        OffsetDateTime nextAttemptAt, OffsetDateTime claimedAt, OffsetDateTime dispatchedAt, OffsetDateTime createdAt
) {
    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(event.getId(), event.getEventKey(), event.getOwnerId(), event.getAggregateType(),
                event.getAggregateId(), event.getEventType(), event.getPayloadJson(), event.getStatus(), event.getAttempts(),
                event.getLastError(), PlatformTime.asUtcOffset(event.getNextAttemptAt()), PlatformTime.asUtcOffset(event.getClaimedAt()),
                PlatformTime.asUtcOffset(event.getDispatchedAt()), PlatformTime.asUtcOffset(event.getCreatedAt()));
    }
}
