package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LocaleNormalizer {

    private static final Set<String> SUPPORTED = Set.of("fr", "en");

    public String normalize(String locale) {
        if (locale == null || locale.isBlank()) {
            return "fr";
        }

        String normalized = locale.trim().toLowerCase().replace('_', '-');
        int separator = normalized.indexOf('-');
        if (separator > 0) {
            normalized = normalized.substring(0, separator);
        }

        return SUPPORTED.contains(normalized) ? normalized : "fr";
    }

    public String defaultLocale() {
        return "fr";
    }

    public boolean isDefault(String locale) {
        return defaultLocale().equals(normalize(locale));
    }
}
