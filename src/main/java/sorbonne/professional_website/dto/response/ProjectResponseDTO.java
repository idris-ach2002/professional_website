package sorbonne.professional_website.dto.response;

import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

public record ProjectResponseDTO(
        Long id,
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
        List<ProjectLinkResponseDTO> links,
        Boolean featured,
        Boolean published,
        Integer displayOrder
) {
}
