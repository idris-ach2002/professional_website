package sorbonne.professional_website.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
class FrontendRedirectService {

    private final String frontendOrigin;
    private final URI frontendOriginUri;

    FrontendRedirectService(@Value("${app.frontend.origin}") String frontendOrigin) {
        if (frontendOrigin == null || frontendOrigin.isBlank()) {
            throw new IllegalStateException("Missing app.frontend.origin / APP_FRONTEND_ORIGIN");
        }

        this.frontendOrigin = stripTrailingSlash(frontendOrigin);
        this.frontendOriginUri = URI.create(this.frontendOrigin);
    }

    String defaultAdminUrl() {
        return frontendOrigin + "/admin";
    }

    String frontendHomeUrl() {
        return frontendOrigin + "/";
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

            return equalsIgnoreCase(frontendOriginUri.getScheme(), requestedUri.getScheme())
                    && equalsIgnoreCase(frontendOriginUri.getHost(), requestedUri.getHost())
                    && effectivePort(frontendOriginUri) == effectivePort(requestedUri);
        } catch (IllegalArgumentException ex) {
            return false;
        }
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
