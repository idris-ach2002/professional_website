package sorbonne.professional_website.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
class FrontendRedirectService {

    private final String defaultFrontendOrigin;
    private final List<URI> allowedFrontendOrigins;

    FrontendRedirectService(
            @Value("${app.frontend.origin}") String defaultFrontendOrigin,
            @Value("${app.frontend.allowed-origins}") String allowedFrontendOrigins
    ) {
        if (defaultFrontendOrigin == null || defaultFrontendOrigin.isBlank()) {
            throw new IllegalStateException("Missing app.frontend.origin / APP_FRONTEND_ORIGIN");
        }

        this.defaultFrontendOrigin = stripTrailingSlash(defaultFrontendOrigin);
        this.allowedFrontendOrigins = parseOrigins(allowedFrontendOrigins);

        if (this.allowedFrontendOrigins.isEmpty()) {
            throw new IllegalStateException("Missing app.frontend.allowed-origins / APP_FRONTEND_ALLOWED_ORIGINS");
        }
    }

    String defaultAdminUrl() {
        return defaultFrontendOrigin + "/admin";
    }

    String frontendHomeUrl() {
        return defaultFrontendOrigin + "/";
    }

    String resolveRedirect(String requestedRedirect) {
        if (isAllowedRedirect(requestedRedirect)) {
            return requestedRedirect;
        }

        return defaultAdminUrl();
    }

    boolean isAllowedRedirect(String requestedRedirect) {
        if (requestedRedirect == null || requestedRedirect.isBlank()) {
            return false;
        }

        try {
            URI requestedUri = URI.create(requestedRedirect);

            if (!requestedUri.isAbsolute()) {
                return false;
            }

            return allowedFrontendOrigins.stream()
                    .anyMatch(allowedOrigin -> sameOrigin(allowedOrigin, requestedUri));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static List<URI> parseOrigins(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return List.of();
        }

        return List.of(rawOrigins.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(FrontendRedirectService::stripTrailingSlash)
                .map(URI::create)
                .toList();
    }

    private static boolean sameOrigin(URI allowedOrigin, URI requestedUri) {
        return equalsIgnoreCase(allowedOrigin.getScheme(), requestedUri.getScheme())
                && equalsIgnoreCase(allowedOrigin.getHost(), requestedUri.getHost())
                && effectivePort(allowedOrigin) == effectivePort(requestedUri);
    }

    private static String stripTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return -1;
    }
}
