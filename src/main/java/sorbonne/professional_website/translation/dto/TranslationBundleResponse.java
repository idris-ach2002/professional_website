package sorbonne.professional_website.translation.dto;

import sorbonne.professional_website.translation.entity.TranslationContentType;
import sorbonne.professional_website.translation.entity.TranslationStatus;

import java.util.List;
import java.util.Map;

public record TranslationBundleResponse(
        TranslationContentType contentType,
        String contentKey,
        String locale,
        String label,
        Map<String, String> sourceFields,
        Map<String, String> translatedFields,
        TranslationStatus status,
        List<String> staleFields
) {
}
