package sorbonne.professional_website.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

public record ProjectRequestDTO(
        @NotBlank(message = "Le titre du projet est obligatoire.")
        @Size(max = 160, message = "Le titre ne doit pas dépasser 160 caractères.")
        String title,

        @Size(max = 300, message = "Le sous-titre ne doit pas dépasser 300 caractères.")
        String subtitle,

        @Size(max = 500, message = "La description courte ne doit pas dépasser 500 caractères.")
        String shortDescription,

        @NotBlank(message = "La description du projet est obligatoire.")
        String description,

        @NotNull(message = "Le statut du projet est obligatoire.")
        ProjectStatus status,

        LocalDate startDate,

        LocalDate endDate,

        @URL(message = "L'URL de l'image est invalide.")
        @Size(max = 512, message = "L'URL de l'image ne doit pas dépasser 512 caractères.")
        String imageUrl,

        @URL(message = "L'URL de démonstration est invalide.")
        @Size(max = 512, message = "L'URL de démonstration ne doit pas dépasser 512 caractères.")
        String demoUrl,

        @URL(message = "L'URL GitHub est invalide.")
        @Size(max = 512, message = "L'URL GitHub ne doit pas dépasser 512 caractères.")
        String githubUrl,

        @URL(message = "L'URL de documentation est invalide.")
        @Size(max = 512, message = "L'URL de documentation ne doit pas dépasser 512 caractères.")
        String documentationUrl,

        List<@NotBlank(message = "Une technologie ne peut pas être vide.")
        @Size(max = 100, message = "Une technologie ne doit pas dépasser 100 caractères.")
                String> stacks,

        List<@NotBlank(message = "Une fonctionnalité ne peut pas être vide.")
        @Size(max = 255, message = "Une fonctionnalité ne doit pas dépasser 255 caractères.")
                String> features,

        @Valid
        List<ProjectLinkRequestDTO> links,

        @NotNull(message = "Le champ featured est obligatoire.")
        Boolean featured,

        @NotNull(message = "Le champ published est obligatoire.")
        Boolean published,

        Integer displayOrder,

        Long websiteVersionId
) {
}