package sorbonne.professional_website.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OwnerRequestDTO(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 256, message = "Le nom ne doit pas dépasser 256 caractères.")
        String name,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 256, message = "Le prénom ne doit pas dépasser 256 caractères.")
        String firstName,

        @NotNull(message = "L'âge est obligatoire.")
        @Min(value = 0, message = "L'âge ne peut pas être négatif.")
        @Max(value = 120, message = "L'âge ne peut pas dépasser 120 ans.")
        Integer age,

        @NotBlank(message = "L'adresse est obligatoire.")
        @Size(max = 256, message = "L'adresse ne doit pas dépasser 256 caractères.")
        String address,

        @Valid
        List<ContactInfoRequestDTO> contacts,

        @Valid
        ProfileRequestDTO prof,

        @Valid
        TimelineRequestDTO timeline,

        @Valid
        List<ProjectRequestDTO> projects
) {
}