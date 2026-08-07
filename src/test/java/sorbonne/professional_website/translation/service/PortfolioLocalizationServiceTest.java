package sorbonne.professional_website.translation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;
import sorbonne.professional_website.translation.entity.TranslationContentType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioLocalizationServiceTest {

    @Mock
    private TranslationStoreService translations;
    @Mock
    private ProvenSkillService provenSkillService;

    private PortfolioLocalizationService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioLocalizationService(translations, new LocaleNormalizer(), provenSkillService);
    }

    @Test
    void keepsFrenchSourceWhenPublishedTranslationIsUnavailable() {
        var published = new TranslationStoreService.PublishedTranslations("en", Map.of());
        when(translations.publishedTranslations("en")).thenReturn(published);
        when(translations.publishedFields(eq(published), eq(TranslationContentType.PROJECT), eq("42"), anyMap()))
                .thenReturn(Map.of());

        ProjectResponseDTO localized = service.localizeProject(sourceProject(), "en");

        assertThat(localized.title()).isEqualTo("Projet français");
        assertThat(localized.description()).isEqualTo("Description française");
        assertThat(localized.features()).containsExactly("Fonction A", "Fonction B");
    }

    @Test
    void appliesPublishedEnglishFieldsAndPreservesTechnicalFields() {
        var published = new TranslationStoreService.PublishedTranslations("en", Map.of());
        when(translations.publishedTranslations("en")).thenReturn(published);
        when(translations.publishedFields(eq(published), eq(TranslationContentType.PROJECT), eq("42"), anyMap()))
                .thenReturn(Map.of(
                        "title", "English project",
                        "description", "English description",
                        "features", "Feature A\nFeature B"
                ));

        ProjectResponseDTO localized = service.localizeProject(sourceProject(), "en-GB");

        assertThat(localized.title()).isEqualTo("English project");
        assertThat(localized.description()).isEqualTo("English description");
        assertThat(localized.features()).containsExactly("Feature A", "Feature B");
        assertThat(localized.githubUrl()).isEqualTo("https://github.com/example/project");
    }

    private ProjectResponseDTO sourceProject() {
        return new ProjectResponseDTO(
                42L,
                "Projet français",
                "Sous-titre",
                "Résumé",
                "Description française",
                ProjectStatus.COMPLETED,
                null,
                null,
                null,
                null,
                "https://github.com/example/project",
                null,
                List.of("Java", "React"),
                List.of("Fonction A", "Fonction B"),
                List.of(),
                true,
                true,
                1,
                "projet-francais"
        );
    }
}
