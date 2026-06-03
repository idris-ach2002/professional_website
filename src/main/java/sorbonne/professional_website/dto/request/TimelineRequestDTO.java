package sorbonne.professional_website.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TimelineRequestDTO(
        @NotBlank(message = "Le titre de la timeline est obligatoire.")
        @Size(max = 120, message = "Le titre de la timeline ne doit pas dépasser 120 caractères.")
        String title,

        @Size(max = 300, message = "La description de la timeline ne doit pas dépasser 300 caractères.")
        String description,

        @Valid
        List<ExperienceRequestDTO> experiences
) {
}