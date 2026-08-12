package sorbonne.professional_website.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveResponseNoStoreFilterTest {

    private final SensitiveResponseNoStoreFilter filter = new SensitiveResponseNoStoreFilter();

    @Test
    void adminResponsesAreExplicitlyNonCacheable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manager/1/versions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store, max-age=0");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getDateHeader("Expires")).isEqualTo(0L);
    }

    @Test
    void publicPortfolioCachePolicyIsNotOverridden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/website/default");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control")).isNull();
    }

    @Test
    void publicUploadedFilesRemainCacheableByTheirOwnControllerPolicy() {
        assertThat(SensitiveResponseNoStoreFilter.isSensitivePath("/uploads/files/photo.webp")).isFalse();
        assertThat(SensitiveResponseNoStoreFilter.isSensitivePath("/uploads")).isTrue();
        assertThat(SensitiveResponseNoStoreFilter.isSensitivePath("/uploads/")).isTrue();
    }
}
