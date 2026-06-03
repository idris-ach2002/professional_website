package sorbonne.professional_website.dto.response;

import java.util.List;

public record TimelineResponseDTO(
        Long id,
        String title,
        String description,
        List<ExperienceResponseDTO> experiences
) {
}
