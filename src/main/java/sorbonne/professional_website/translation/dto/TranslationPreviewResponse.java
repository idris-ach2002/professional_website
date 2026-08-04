package sorbonne.professional_website.translation.dto;

import java.util.Map;

public record TranslationPreviewResponse(
        String provider,
        String sourceLocale,
        String targetLocale,
        Map<String, String> translatedFields
) {
}
