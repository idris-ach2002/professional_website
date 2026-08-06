package sorbonne.professional_website.translation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.translation.dto.TranslationPreviewRequest;
import sorbonne.professional_website.translation.entity.ContentTranslation;
import sorbonne.professional_website.translation.entity.TranslationContentType;
import sorbonne.professional_website.translation.entity.TranslationStatus;
import sorbonne.professional_website.translation.repository.ContentTranslationRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationStoreServiceTest {

    @Mock
    private ContentTranslationRepository repository;
    @Mock
    private TranslatableContentService contentService;
    @Mock
    private LibreTranslateClient libreTranslateClient;

    private TranslationHashService hashService;
    private TranslationStoreService service;

    @BeforeEach
    void setUp() {
        hashService = new TranslationHashService();
        service = new TranslationStoreService(
                repository,
                contentService,
                hashService,
                new LocaleNormalizer(),
                libreTranslateClient
        );
    }

    @Test
    void previewDelegatesToLibreTranslate() {
        Map<String, String> sourceFields = Map.of("title", "Bonjour");
        Map<String, String> translatedFields = Map.of("title", "Hello");
        when(libreTranslateClient.translate(sourceFields, "fr", "en")).thenReturn(translatedFields);

        var response = service.preview(new TranslationPreviewRequest("fr-FR", "en-GB", sourceFields));

        assertThat(response.translatedFields()).containsEntry("title", "Hello");
        verify(libreTranslateClient).translate(sourceFields, "fr", "en");
    }

    @Test
    void previewRejectsIdenticalLocales() {
        assertThatThrownBy(() -> service.preview(
                new TranslationPreviewRequest("fr", "fr-FR", Map.of("title", "Bonjour"))
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publishedFieldsReturnsCompleteFreshPublishedTranslation() {
        TranslatableContent source = projectSource();
        when(contentService.get(TranslationContentType.PROJECT, "42")).thenReturn(source);
        when(repository.findByContentTypeAndContentKeyAndLocaleAndStatus(
                TranslationContentType.PROJECT,
                "42",
                "en",
                TranslationStatus.PUBLISHED
        )).thenReturn(List.of(
                translation("title", "Hello", hashService.hash("Bonjour")),
                translation("description", "Technical project", hashService.hash("Projet technique"))
        ));

        Map<String, String> fields = service.publishedFields(TranslationContentType.PROJECT, "42", "en");

        assertThat(fields)
                .containsEntry("title", "Hello")
                .containsEntry("description", "Technical project");
    }

    @Test
    void publishedFieldsFallsBackWhenTranslationIsStale() {
        TranslatableContent source = projectSource();
        when(contentService.get(TranslationContentType.PROJECT, "42")).thenReturn(source);
        when(repository.findByContentTypeAndContentKeyAndLocaleAndStatus(
                TranslationContentType.PROJECT,
                "42",
                "en",
                TranslationStatus.PUBLISHED
        )).thenReturn(List.of(
                translation("title", "Hello", hashService.hash("Ancien titre")),
                translation("description", "Technical project", hashService.hash("Projet technique"))
        ));

        assertThat(service.publishedFields(TranslationContentType.PROJECT, "42", "en")).isEmpty();
    }

    @Test
    void publishedFieldsNeverQueriesTranslationTableForFrench() {
        assertThat(service.publishedFields(TranslationContentType.PROJECT, "42", "fr-FR")).isEmpty();
    }

    private TranslatableContent projectSource() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "Bonjour");
        fields.put("description", "Projet technique");
        return new TranslatableContent(TranslationContentType.PROJECT, "42", "Projet", fields);
    }

    private ContentTranslation translation(String fieldName, String value, String sourceHash) {
        return ContentTranslation.builder()
                .contentType(TranslationContentType.PROJECT)
                .contentKey("42")
                .locale("en")
                .fieldName(fieldName)
                .translatedText(value)
                .sourceHash(sourceHash)
                .status(TranslationStatus.PUBLISHED)
                .build();
    }
}
