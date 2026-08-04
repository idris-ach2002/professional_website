package sorbonne.professional_website.translation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "translation.libretranslate")
public record LibreTranslateProperties(
        boolean enabled,
        String baseUrl,
        Duration timeout
) {
    public LibreTranslateProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:5000" : baseUrl.replaceAll("/+$", "");
        timeout = timeout == null ? Duration.ofSeconds(45) : timeout;
    }
}
