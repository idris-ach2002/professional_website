package sorbonne.professional_website.analytics.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsRateLimiterTest {

    @Test
    void rejectsRequestsAfterTheConfiguredWindowCapacity() {
        AnalyticsRateLimiter limiter = new AnalyticsRateLimiter(10, Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.10");

        for (int index = 0; index < 10; index++) assertThat(limiter.allow(request)).isTrue();
        assertThat(limiter.allow(request)).isFalse();
    }
}
