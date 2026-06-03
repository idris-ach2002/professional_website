package sorbonne.professional_website.dto;

import sorbonne.professional_website.entity.enumerations.ProjectLinkType;

public record ProjectLinkDTO(
        ProjectLinkType type,
        String label,
        String url
) {
}