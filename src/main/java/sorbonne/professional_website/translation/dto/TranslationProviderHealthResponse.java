package sorbonne.professional_website.translation.dto;

public record TranslationProviderHealthResponse(
        boolean enabled,
        boolean reachable,
        String provider,
        String baseUrl,
        String message
) {
}
