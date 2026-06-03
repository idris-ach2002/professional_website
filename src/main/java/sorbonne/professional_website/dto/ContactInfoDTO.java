package sorbonne.professional_website.dto;

import sorbonne.professional_website.entity.enumerations.Contact;

public record ContactInfoDTO(
        Contact type,
        String value
) {
}