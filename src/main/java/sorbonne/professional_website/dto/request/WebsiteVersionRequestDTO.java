package sorbonne.professional_website.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WebsiteVersionRequestDTO(
        @Size(max = 80, message = "Le tag de version ne doit pas dépasser 80 caractères.")
        String versionTag,

        @Size(max = 160, message = "Le libellé de version ne doit pas dépasser 160 caractères.")
        String label,

        @Size(max = 500, message = "La description de version ne doit pas dépasser 500 caractères.")
        String description,

        /**
         * Optional on creation. If true, the service deactivates every other version of the same owner
         * before activating this one. Updates should use the dedicated /activate endpoint instead.
         */
        Boolean active,

        Boolean published,

        @Valid
        ProfileRequestDTO prof,

        @Valid
        TimelineRequestDTO timeline,

        @Valid
        List<ProjectRequestDTO> projects
) {
}
