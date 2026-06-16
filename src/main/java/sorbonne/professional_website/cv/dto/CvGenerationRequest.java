package sorbonne.professional_website.cv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record CvGenerationRequest(
        @Size(max = 120)
        String templateId,

        @Size(max = 10)
        String language,

        @Valid
        CvThemeDto theme,

        @Valid
        CvSectionsDto sections,

        Integer projectLimit,

        Integer experienceLimit,

        @Size(max = 120_000)
        String latexSourceOverride
) {
}
