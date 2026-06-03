package sorbonne.professional_website.dto;

import java.util.List;

public record TimelineDTO(
        String title,
        String description,
        List<ExperienceDTO> experiences
) {
}