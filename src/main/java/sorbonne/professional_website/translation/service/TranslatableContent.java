package sorbonne.professional_website.translation.service;

import sorbonne.professional_website.translation.entity.TranslationContentType;

import java.util.Map;

public record TranslatableContent(
        TranslationContentType contentType,
        String contentKey,
        String label,
        Map<String, String> fields
) {
}
