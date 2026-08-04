package sorbonne.professional_website.translation.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sorbonne.professional_website.translation.config.LibreTranslateProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LibreTranslateClient {

    private final RestClient restClient;
    private final LibreTranslateProperties properties;

    public LibreTranslateClient(RestClient libreTranslateRestClient, LibreTranslateProperties properties) {
        this.restClient = libreTranslateRestClient;
        this.properties = properties;
    }

    public Map<String, String> translate(Map<String, String> fields, String sourceLocale, String targetLocale) {
        if (!properties.enabled()) {
            throw new IllegalStateException("LibreTranslate is disabled");
        }

        Map<String, String> translated = new LinkedHashMap<>();
        fields.forEach((field, value) -> {
            if (value == null || value.isBlank()) {
                translated.put(field, value);
                return;
            }
            translated.put(field, translatePreservingLines(value, sourceLocale, targetLocale));
        });
        return translated;
    }

    public boolean isReachable() {
        if (!properties.enabled()) return false;
        try {
            String body = restClient.get()
                    .uri("/languages")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            return body != null && !body.isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String translatePreservingLines(String text, String sourceLocale, String targetLocale) {
        if (!text.contains("\n") && !text.contains("\r")) {
            return translateText(text, sourceLocale, targetLocale);
        }

        return text.lines()
                .map(line -> line.isBlank() ? "" : translateText(line, sourceLocale, targetLocale))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String translateText(String text, String sourceLocale, String targetLocale) {
        LibreTranslateRequest request = new LibreTranslateRequest(text, sourceLocale, targetLocale, "text");
        LibreTranslateResponse response = restClient.post()
                .uri("/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(LibreTranslateResponse.class);

        if (response == null || response.translatedText() == null) {
            throw new IllegalStateException("LibreTranslate returned an empty response");
        }
        return response.translatedText();
    }

    private record LibreTranslateRequest(String q, String source, String target, String format) {
    }

    private record LibreTranslateResponse(String translatedText) {
    }
}
