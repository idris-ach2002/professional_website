package sorbonne.professional_website.translation.dto;

import jakarta.validation.constraints.NotNull;
import sorbonne.professional_website.translation.entity.TranslationStatus;

public record TranslationAutoRequest(
        @NotNull TranslationStatus status
) {
}
