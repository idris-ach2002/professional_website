package sorbonne.professional_website.dto.request;

import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

public record ProjectRequestDTO(
        String title,
        String subtitle,
        String shortDescription,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String imageUrl,
        String demoUrl,
        String githubUrl,
        String documentationUrl,
        List<String> stacks,
        List<String> features,
        List<ProjectLinkRequestDTO> links,
        boolean featured,
        boolean published,
        Integer displayOrder
) {
}
