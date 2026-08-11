package sorbonne.professional_website.translation.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sorbonne.professional_website.translation.config.LibreTranslateProperties;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Component
public class LibreTranslateClient {

    private final RestClient restClient;
    private final LibreTranslateProperties properties;
    private final Executor virtualIoExecutor;

    public LibreTranslateClient(
            RestClient libreTranslateRestClient,
            LibreTranslateProperties properties,
            @Qualifier("virtualIoExecutor") Executor virtualIoExecutor
    ) {
        this.restClient = libreTranslateRestClient;
        this.properties = properties;
        this.virtualIoExecutor = virtualIoExecutor;
    }

    /**
     * Independent text fields are translated concurrently on the bounded I/O
     * virtual-thread I/O lane. Inputs are immutable snapshots; no JPA entity crosses a thread
     * boundary and output order remains deterministic.
     */
    public Map<String, String> translate(Map<String, String> fields, String sourceLocale, String targetLocale) {
        if (!properties.enabled()) throw new IllegalStateException("LibreTranslate is disabled");
        if (fields == null || fields.isEmpty()) return Map.of();

        List<Map.Entry<String, String>> snapshot = List.copyOf(fields.entrySet());
        List<CompletableFuture<Map.Entry<String, String>>> futures = snapshot.stream()
                .map(entry -> CompletableFuture.<Map.Entry<String, String>>supplyAsync(
                        () -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), translateValue(entry.getValue(), sourceLocale, targetLocale)),
                        virtualIoExecutor
                ))
                .toList();

        long timeoutMillis = Math.max(1_000L, properties.timeout().toMillis());
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .join();
            Map<String, String> translated = new LinkedHashMap<>();
            futures.stream().map(CompletableFuture::join)
                    .forEach(entry -> translated.put(entry.getKey(), entry.getValue()));
            return translated;
        } catch (RuntimeException exception) {
            futures.forEach(future -> future.cancel(true));
            throw exception;
        }
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

    private String translateValue(String value, String sourceLocale, String targetLocale) {
        if (value == null || value.isBlank()) return value;
        return translatePreservingLines(value, sourceLocale, targetLocale);
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
