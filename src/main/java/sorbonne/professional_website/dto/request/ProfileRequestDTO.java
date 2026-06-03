package sorbonne.professional_website.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ProfileRequestDTO(
        @NotBlank(message = "Le titre du profil est obligatoire.")
        @Size(max = 120, message = "Le titre ne doit pas dépasser 120 caractères.")
        String title,

        @Size(max = 180, message = "Le sous-titre ne doit pas dépasser 180 caractères.")
        String subtitle,

        @Size(max = 256, message = "Le headline ne doit pas dépasser 256 caractères.")
        String headline,

        @Size(max = 500, message = "La description courte ne doit pas dépasser 500 caractères.")
        String shortDescription,

        @NotBlank(message = "La description du profil est obligatoire.")
        String description,

        @Size(max = 120, message = "La localisation ne doit pas dépasser 120 caractères.")
        String location,

        @Size(max = 160, message = "La disponibilité ne doit pas dépasser 160 caractères.")
        String availability,

        @URL(message = "L'URL de l'image de profil est invalide.")
        @Size(max = 512, message = "L'URL de l'image de profil ne doit pas dépasser 512 caractères.")
        String profileImageUrl,

        @URL(message = "L'URL du logo est invalide.")
        @Size(max = 512, message = "L'URL du logo ne doit pas dépasser 512 caractères.")
        String logoUrl,

        @URL(message = "L'URL du CV est invalide.")
        @Size(max = 512, message = "L'URL du CV ne doit pas dépasser 512 caractères.")
        String cvUrl,

        @URL(message = "L'URL du portfolio est invalide.")
        @Size(max = 512, message = "L'URL du portfolio ne doit pas dépasser 512 caractères.")
        String portfolioUrl
) {
}