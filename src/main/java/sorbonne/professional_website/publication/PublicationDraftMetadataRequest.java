package sorbonne.professional_website.publication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicationDraftMetadataRequest(
        @NotBlank(message = "Le libellé de la version est obligatoire.")
        @Size(max = 160, message = "Le libellé de version ne doit pas dépasser 160 caractères.")
        String label,

        @Size(max = 500, message = "La description de version ne doit pas dépasser 500 caractères.")
        String description
) {}
