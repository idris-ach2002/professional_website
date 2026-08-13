package sorbonne.professional_website.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigCorsTest {

    @Test
    void optimisticConcurrencyHeadersAreAllowedAndExposedCrossOrigin() {
        WebSecurityConfig config = new WebSecurityConfig();
        UrlBasedCorsConfigurationSource source = config.corsConfigurationSource("https://front.example.test");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/manager/1/versions/2");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedHeaders()).contains("If-Match", "Idempotency-Key");
        assertThat(cors.getExposedHeaders()).contains("ETag");
        assertThat(cors.getAllowedOrigins()).containsExactly("https://front.example.test");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
