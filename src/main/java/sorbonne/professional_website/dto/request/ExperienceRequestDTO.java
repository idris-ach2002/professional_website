package sorbonne.professional_website.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;

import java.time.LocalDate;
import java.util.List;

public record ExperienceRequestDTO(
        @NotNull(message = "La catégorie de l'expérience est obligatoire.")
        CategoryExperience category,

        @NotBlank(message = "Le titre de l'expérience est obligatoire.")
        @Size(max = 160, message = "Le titre ne doit pas dépasser 160 caractères.")
        String title,

        @Size(max = 160, message = "L'organisation ne doit pas dépasser 160 caractères.")
        String organization,

        @Size(max = 160, message = "La localisation ne doit pas dépasser 160 caractères.")
        String location,

        @Size(max = 500, message = "Le résumé ne doit pas dépasser 500 caractères.")
        String summary,

        String description,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Le statut currentPosition est obligatoire.")
        Boolean currentPosition,

        @URL(message = "L'URL de l'image est invalide.")
        @Size(max = 512, message = "L'URL de l'image ne doit pas dépasser 512 caractères.")
        String imageUrl,

        @URL(message = "L'URL du site web est invalide.")
        @Size(max = 512, message = "L'URL du site web ne doit pas dépasser 512 caractères.")
        String websiteUrl,

        List<@NotBlank(message = "Une compétence ne peut pas être vide.")
        @Size(max = 100, message = "Une compétence ne doit pas dépasser 100 caractères.")
                String> skills,

        Integer displayOrder
) {
}