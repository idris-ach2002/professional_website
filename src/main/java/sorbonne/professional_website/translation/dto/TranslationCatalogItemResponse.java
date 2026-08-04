package sorbonne.professional_website.translation.dto;

import sorbonne.professional_website.translation.entity.TranslationContentType;
import sorbonne.professional_website.translation.entity.TranslationStatus;

public record TranslationCatalogItemResponse(
        TranslationContentType contentType,
        String contentKey,
        String label,
        String locale,
        TranslationStatus status,
        boolean stale,
        int translatedFieldCount,
        int sourceFieldCount
) {
}
