package sorbonne.professional_website.translation.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.translation.config.LibreTranslateProperties;
import sorbonne.professional_website.translation.dto.*;
import sorbonne.professional_website.translation.entity.TranslationContentType;
import sorbonne.professional_website.translation.service.LibreTranslateClient;
import sorbonne.professional_website.translation.service.TranslationStoreService;

import java.util.List;

@RestController
@RequestMapping("/api/translations")
public class TranslationAdminController {

    private final TranslationStoreService service;
    private final LibreTranslateClient client;
    private final LibreTranslateProperties properties;

    public TranslationAdminController(
            TranslationStoreService service,
            LibreTranslateClient client,
            LibreTranslateProperties properties
    ) {
        this.service = service;
        this.client = client;
        this.properties = properties;
    }

    @GetMapping("/provider/health")
    public ResponseEntity<TranslationProviderHealthResponse> providerHealth() {
        boolean reachable = client.isReachable();
        return ResponseEntity.ok(new TranslationProviderHealthResponse(
                properties.enabled(),
                reachable,
                "LibreTranslate",
                properties.baseUrl(),
                reachable ? "LibreTranslate is reachable" : "LibreTranslate is unavailable"
        ));
    }

    @PostMapping("/preview")
    public ResponseEntity<TranslationPreviewResponse> preview(
            @RequestBody @Valid TranslationPreviewRequest request
    ) {
        return ResponseEntity.ok(service.preview(request));
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<TranslationCatalogItemResponse>> catalog(
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(service.catalog(locale));
    }

    @PostMapping("/{contentType}/{contentKey}/auto")
    public ResponseEntity<TranslationBundleResponse> autoTranslate(
            @PathVariable TranslationContentType contentType,
            @PathVariable String contentKey,
            @RequestParam(defaultValue = "en") String locale,
            @RequestBody @Valid TranslationAutoRequest request
    ) {
        return ResponseEntity.ok(service.autoTranslate(contentType, contentKey, locale, request));
    }

    @GetMapping("/{contentType}/{contentKey}")
    public ResponseEntity<TranslationBundleResponse> getBundle(
            @PathVariable TranslationContentType contentType,
            @PathVariable String contentKey,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(service.getBundle(contentType, contentKey, locale));
    }

    @PutMapping("/{contentType}/{contentKey}")
    public ResponseEntity<TranslationBundleResponse> save(
            @PathVariable TranslationContentType contentType,
            @PathVariable String contentKey,
            @RequestParam(defaultValue = "en") String locale,
            @RequestBody @Valid TranslationSaveRequest request
    ) {
        return ResponseEntity.ok(service.save(contentType, contentKey, locale, request));
    }
}
