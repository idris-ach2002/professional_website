package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.translation.dto.*;
import sorbonne.professional_website.translation.entity.ContentTranslation;
import sorbonne.professional_website.translation.entity.TranslationContentType;
import sorbonne.professional_website.translation.entity.TranslationStatus;
import sorbonne.professional_website.translation.repository.ContentTranslationRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TranslationStoreService {

    private final ContentTranslationRepository repository;
    private final TranslatableContentService contentService;
    private final TranslationHashService hashService;
    private final LocaleNormalizer localeNormalizer;
    private final LibreTranslateClient libreTranslateClient;

    public TranslationStoreService(
            ContentTranslationRepository repository,
            TranslatableContentService contentService,
            TranslationHashService hashService,
            LocaleNormalizer localeNormalizer,
            LibreTranslateClient libreTranslateClient
    ) {
        this.repository = repository;
        this.contentService = contentService;
        this.hashService = hashService;
        this.localeNormalizer = localeNormalizer;
        this.libreTranslateClient = libreTranslateClient;
    }

    public TranslationPreviewResponse preview(TranslationPreviewRequest request) {
        String source = localeNormalizer.normalize(request.sourceLocale());
        String target = localeNormalizer.normalize(request.targetLocale());
        if (source.equals(target)) {
            throw new IllegalArgumentException("Source and target locales must differ");
        }
        return new TranslationPreviewResponse(
                "LibreTranslate",
                source,
                target,
                libreTranslateClient.translate(request.fields(), source, target)
        );
    }

    @Transactional
    public TranslationBundleResponse autoTranslate(
            TranslationContentType type,
            String key,
            String locale,
            TranslationAutoRequest request
    ) {
        String normalizedLocale = localeNormalizer.normalize(locale);
        if (localeNormalizer.isDefault(normalizedLocale)) {
            throw new IllegalArgumentException("The automatic translation target must differ from fr");
        }

        TranslatableContent source = contentService.get(type, key);
        Map<String, String> translatedFields = libreTranslateClient.translate(
                source.fields(),
                localeNormalizer.defaultLocale(),
                normalizedLocale
        );

        return save(
                type,
                key,
                normalizedLocale,
                new TranslationSaveRequest(translatedFields, request.status())
        );
    }

    @Transactional
    public TranslationBundleResponse save(
            TranslationContentType type,
            String key,
            String locale,
            TranslationSaveRequest request
    ) {
        String normalizedLocale = localeNormalizer.normalize(locale);
        if (localeNormalizer.isDefault(normalizedLocale)) {
            throw new IllegalArgumentException("The source locale fr is stored in the business tables, not in content_translation");
        }

        TranslatableContent source = contentService.get(type, key);
        if (request.status() == TranslationStatus.PUBLISHED) {
            List<String> missingFields = source.fields().keySet().stream()
                    .filter(field -> {
                        String value = request.fields().get(field);
                        return value == null || value.isBlank();
                    })
                    .toList();
            if (!missingFields.isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot publish an incomplete translation. Missing fields: "
                                + String.join(", ", missingFields)
                );
            }
        }

        for (Map.Entry<String, String> entry : request.fields().entrySet()) {
            String fieldName = entry.getKey();
            if (!source.fields().containsKey(fieldName)) {
                throw new IllegalArgumentException("Unknown translatable field: " + fieldName);
            }

            String translatedText = entry.getValue();
            if (translatedText == null || translatedText.isBlank()) {
                repository.findByContentTypeAndContentKeyAndLocaleAndFieldName(type, key, normalizedLocale, fieldName)
                        .ifPresent(repository::delete);
                continue;
            }

            ContentTranslation translation = repository
                    .findByContentTypeAndContentKeyAndLocaleAndFieldName(type, key, normalizedLocale, fieldName)
                    .orElseGet(ContentTranslation::new);
            translation.setContentType(type);
            translation.setContentKey(key);
            translation.setLocale(normalizedLocale);
            translation.setFieldName(fieldName);
            translation.setTranslatedText(translatedText.trim());
            translation.setSourceHash(hashService.hash(source.fields().get(fieldName)));
            translation.setStatus(request.status());
            repository.save(translation);
        }

        return getBundle(type, key, normalizedLocale);
    }

    @Transactional(readOnly = true)
    public TranslationBundleResponse getBundle(TranslationContentType type, String key, String locale) {
        String normalizedLocale = localeNormalizer.normalize(locale);
        TranslatableContent source = contentService.get(type, key);
        List<ContentTranslation> translations = repository.findByContentTypeAndContentKeyAndLocale(type, key, normalizedLocale);
        Map<String, ContentTranslation> byField = translations.stream()
                .collect(Collectors.toMap(ContentTranslation::getFieldName, Function.identity(), (left, right) -> right));

        Map<String, String> translatedFields = new LinkedHashMap<>();
        List<String> staleFields = new ArrayList<>();
        TranslationStatus overallStatus = TranslationStatus.DRAFT;
        boolean hasPublished = false;

        for (Map.Entry<String, String> sourceEntry : source.fields().entrySet()) {
            ContentTranslation translation = byField.get(sourceEntry.getKey());
            if (translation == null) continue;
            translatedFields.put(sourceEntry.getKey(), translation.getTranslatedText());
            if (!Objects.equals(translation.getSourceHash(), hashService.hash(sourceEntry.getValue()))) {
                staleFields.add(sourceEntry.getKey());
            }
            if (translation.getStatus() == TranslationStatus.PUBLISHED) {
                hasPublished = true;
            }
        }
        if (hasPublished
                && translatedFields.size() == source.fields().size()
                && staleFields.isEmpty()
                && source.fields().keySet().stream()
                        .map(byField::get)
                        .allMatch(item -> item != null && item.getStatus() == TranslationStatus.PUBLISHED)) {
            overallStatus = TranslationStatus.PUBLISHED;
        }

        return new TranslationBundleResponse(
                type,
                key,
                normalizedLocale,
                source.label(),
                source.fields(),
                translatedFields,
                overallStatus,
                staleFields
        );
    }

    /**
     * Loads every published translation for one locale in a single repository query.
     * Public portfolio rendering reuses this immutable index instead of issuing one
     * SQL query per profile, timeline, experience, project and proven skill.
     */
    @Transactional(readOnly = true)
    public PublishedTranslations publishedTranslations(String locale) {
        String normalizedLocale = localeNormalizer.normalize(locale);
        if (localeNormalizer.isDefault(normalizedLocale)) {
            return PublishedTranslations.empty(normalizedLocale);
        }

        Map<TranslationKey, Map<String, PublishedField>> byContent = new LinkedHashMap<>();
        for (ContentTranslation translation : repository.findByLocaleAndStatus(
                normalizedLocale,
                TranslationStatus.PUBLISHED
        )) {
            TranslationKey key = new TranslationKey(
                    translation.getContentType(),
                    translation.getContentKey()
            );
            byContent.computeIfAbsent(key, ignored -> new LinkedHashMap<>())
                    .put(translation.getFieldName(), new PublishedField(
                            translation.getTranslatedText(),
                            translation.getSourceHash()
                    ));
        }

        Map<TranslationKey, Map<String, PublishedField>> immutable = byContent.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> Map.copyOf(entry.getValue())
                ));
        return new PublishedTranslations(normalizedLocale, immutable);
    }

    public Map<String, String> publishedFields(
            PublishedTranslations publishedTranslations,
            TranslationContentType type,
            String key,
            Map<String, String> sourceFields
    ) {
        if (publishedTranslations == null
                || localeNormalizer.isDefault(publishedTranslations.locale())
                || sourceFields == null
                || sourceFields.isEmpty()) {
            return Map.of();
        }

        Map<String, PublishedField> translated = publishedTranslations.byContent()
                .get(new TranslationKey(type, key));
        if (translated == null || translated.isEmpty()) return Map.of();

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> sourceEntry : sourceFields.entrySet()) {
            PublishedField field = translated.get(sourceEntry.getKey());
            if (field == null
                    || field.text() == null
                    || field.text().isBlank()
                    || !Objects.equals(field.sourceHash(), hashService.hash(sourceEntry.getValue()))) {
                return Map.of();
            }
            result.put(sourceEntry.getKey(), field.text());
        }
        return result;
    }

    public record PublishedTranslations(
            String locale,
            Map<TranslationKey, Map<String, PublishedField>> byContent
    ) {
        static PublishedTranslations empty(String locale) {
            return new PublishedTranslations(locale, Map.of());
        }
    }

    public record TranslationKey(TranslationContentType type, String key) {
    }

    public record PublishedField(String text, String sourceHash) {
    }

    @Transactional(readOnly = true)
    public Map<String, String> publishedFields(TranslationContentType type, String key, String locale) {
        String normalizedLocale = localeNormalizer.normalize(locale);
        if (localeNormalizer.isDefault(normalizedLocale)) return Map.of();

        TranslatableContent source;
        try {
            source = contentService.get(type, key);
        } catch (RuntimeException exception) {
            return Map.of();
        }

        Map<String, ContentTranslation> byField = repository
                .findByContentTypeAndContentKeyAndLocaleAndStatus(
                        type,
                        key,
                        normalizedLocale,
                        TranslationStatus.PUBLISHED
                )
                .stream()
                .collect(Collectors.toMap(
                        ContentTranslation::getFieldName,
                        Function.identity(),
                        (left, right) -> right
                ));

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> sourceEntry : source.fields().entrySet()) {
            ContentTranslation translation = byField.get(sourceEntry.getKey());
            if (translation == null
                    || translation.getTranslatedText() == null
                    || translation.getTranslatedText().isBlank()
                    || !Objects.equals(translation.getSourceHash(), hashService.hash(sourceEntry.getValue()))) {
                return Map.of();
            }
            result.put(sourceEntry.getKey(), translation.getTranslatedText());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TranslationCatalogItemResponse> catalog(String locale) {
        String normalizedLocale = localeNormalizer.normalize(locale);
        return contentService.catalog().stream()
                .map(content -> {
                    TranslationBundleResponse bundle = getBundle(content.contentType(), content.contentKey(), normalizedLocale);
                    return new TranslationCatalogItemResponse(
                            content.contentType(),
                            content.contentKey(),
                            content.label(),
                            normalizedLocale,
                            bundle.status(),
                            !bundle.staleFields().isEmpty(),
                            bundle.translatedFields().size(),
                            bundle.sourceFields().size()
                    );
                })
                .sorted(Comparator
                        .comparing((TranslationCatalogItemResponse item) -> item.contentType().name())
                        .thenComparing(TranslationCatalogItemResponse::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
