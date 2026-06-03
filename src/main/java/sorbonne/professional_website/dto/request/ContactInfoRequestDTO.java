package sorbonne.professional_website.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sorbonne.professional_website.entity.enumerations.Contact;

public record ContactInfoRequestDTO(
        @NotNull(message = "Le type de contact est obligatoire.")
        Contact type,

        @NotBlank(message = "La valeur du contact est obligatoire.")
        String value
) {
}