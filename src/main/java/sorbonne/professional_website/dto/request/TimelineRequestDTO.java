package sorbonne.professional_website.dto.request;

import java.util.List;

public record TimelineRequestDTO(
        String title,
        String description,
        List<ExperienceRequestDTO> experiences
) {
}
