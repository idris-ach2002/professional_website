package sorbonne.professional_website.dto.request;

import sorbonne.professional_website.entity.enumerations.ProjectLinkType;

public record ProjectLinkRequestDTO(
        ProjectLinkType type,
        String label,
        String url
) {
}
