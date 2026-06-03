package sorbonne.professional_website.dto.request;

import sorbonne.professional_website.entity.enumerations.Contact;

public record ContactInfoRequestDTO(
        Contact type,
        String value
) {
}
