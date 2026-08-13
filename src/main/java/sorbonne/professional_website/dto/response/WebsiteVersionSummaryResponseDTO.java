package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import sorbonne.professional_website.publication.PublicationStatus;

public record WebsiteVersionSummaryResponseDTO(
        Long id,
        long contentRevision,
        String versionTag,
        String label,
        String description,
        Boolean active,
        Boolean published,
        PublicationStatus publicationStatus,
        OffsetDateTime scheduledAt,
        OffsetDateTime publishedAt,
        String publicationError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
