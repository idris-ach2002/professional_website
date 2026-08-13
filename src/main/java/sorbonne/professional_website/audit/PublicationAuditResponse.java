package sorbonne.professional_website.audit;

import java.time.OffsetDateTime;
import sorbonne.professional_website.time.PlatformTime;

public record PublicationAuditResponse(
        String id,
        Long ownerId,
        Long versionId,
        String action,
        String actor,
        String correlationId,
        String beforeJson,
        String afterJson,
        String metadataJson,
        OffsetDateTime createdAt
) {
    public static PublicationAuditResponse from(PublicationAuditEntry entry) {
        return new PublicationAuditResponse(
                entry.getId(), entry.getOwnerId(), entry.getVersionId(), entry.getAction(), entry.getActor(),
                entry.getCorrelationId(), entry.getBeforeJson(), entry.getAfterJson(), entry.getMetadataJson(),
                PlatformTime.asUtcOffset(entry.getCreatedAt())
        );
    }
}
