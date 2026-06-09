package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record WebsiteVersionResponseDTO(
        Long id,
        String versionTag,
        String label,
        String description,
        Boolean active,
        Boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ProfileResponseDTO prof,
        TimelineResponseDTO timeline,
        List<ProjectResponseDTO> projects
) {
}
