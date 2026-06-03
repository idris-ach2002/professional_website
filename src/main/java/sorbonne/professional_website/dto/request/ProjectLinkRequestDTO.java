package sorbonne.professional_website.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import sorbonne.professional_website.entity.enumerations.ProjectLinkType;

public record ProjectLinkRequestDTO(
        @NotNull(message = "Le type de lien est obligatoire.")
        ProjectLinkType type,

        @Size(max = 100, message = "Le label ne doit pas dépasser 100 caractères.")
        String label,

        @NotBlank(message = "L'URL du lien est obligatoire.")
        @URL(message = "L'URL du lien est invalide.")
        @Size(max = 512, message = "L'URL du lien ne doit pas dépasser 512 caractères.")
        String url
) {
}