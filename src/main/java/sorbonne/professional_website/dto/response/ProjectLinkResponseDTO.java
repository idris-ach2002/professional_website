package sorbonne.professional_website.dto.response;

import sorbonne.professional_website.entity.enumerations.ProjectLinkType;

public record ProjectLinkResponseDTO(
        ProjectLinkType type,
        String label,
        String url
) {
}
