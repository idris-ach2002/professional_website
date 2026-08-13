package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import sorbonne.professional_website.publication.PublicationStatus;

public record WebsiteVersionResponseDTO(
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
        LocalDateTime updatedAt,
        ProfileResponseDTO prof,
        TimelineResponseDTO timeline,
        List<ProjectResponseDTO> projects
) {
}
