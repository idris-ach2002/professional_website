package sorbonne.professional_website.translation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import sorbonne.professional_website.translation.entity.TranslationStatus;

import java.util.Map;

public record TranslationSaveRequest(
        @NotEmpty Map<String, String> fields,
        @NotNull TranslationStatus status
) {
}
