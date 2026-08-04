package sorbonne.professional_website.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record TranslationPreviewRequest(
        @NotBlank String sourceLocale,
        @NotBlank String targetLocale,
        @NotEmpty Map<@NotBlank String, @Size(max = 20000) String> fields
) {
}
