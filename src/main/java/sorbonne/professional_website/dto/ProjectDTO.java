package sorbonne.professional_website.dto;

import java.time.LocalDate;
import java.util.List;

public record ProjectDTO(
        Long id,
        String title,
        String subtitle,
        String shortDescription,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String imageUrl,
        String demoUrl,
        String githubUrl,
        String documentationUrl,
        List<String> stacks,
        List<String> features,
        List<ProjectLinkDTO> links,
        Integer displayOrder
) {
}