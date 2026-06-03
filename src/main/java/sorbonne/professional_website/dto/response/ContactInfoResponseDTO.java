package sorbonne.professional_website.dto.response;

import sorbonne.professional_website.entity.enumerations.Contact;

public record ContactInfoResponseDTO(
        Contact type,
        String value
) {
}
