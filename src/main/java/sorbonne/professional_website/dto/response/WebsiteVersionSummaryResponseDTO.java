package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;

public record WebsiteVersionSummaryResponseDTO(
        Long id,
        String versionTag,
        String label,
        String description,
        Boolean active,
        Boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
