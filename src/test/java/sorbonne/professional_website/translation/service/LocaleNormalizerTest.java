package sorbonne.professional_website.translation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocaleNormalizerTest {

    private final LocaleNormalizer normalizer = new LocaleNormalizer();

    @Test
    void normalizesSupportedLocalesAndRegionalVariants() {
        assertThat(normalizer.normalize("EN_gb")).isEqualTo("en");
        assertThat(normalizer.normalize("fr-FR")).isEqualTo("fr");
    }

    @Test
    void fallsBackToFrenchForBlankOrUnsupportedLocales() {
        assertThat(normalizer.normalize(null)).isEqualTo("fr");
        assertThat(normalizer.normalize("  ")).isEqualTo("fr");
        assertThat(normalizer.normalize("de")).isEqualTo("fr");
    }
}
