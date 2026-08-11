package sorbonne.professional_website.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;
import sorbonne.professional_website.analytics.repository.AnalyticsEventRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsAggregationIntegrationTest {

    @Autowired AnalyticsEventRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void databaseAggregatesSummaryWithoutLoadingTheWholeEventWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        repository.saveAllAndFlush(List.of(
                event("page_view", "/", "visitor-a", "session-a", now.minusMinutes(4)),
                event("page_view", "/projects", "visitor-a", "session-a", now.minusMinutes(3)),
                event("project_view", "/projects", "visitor-b", "session-b", now.minusMinutes(2)),
                event("github_click", "/", "visitor-b", "session-b", now.minusMinutes(1))
        ));

        var totals = repository.aggregateTotals(now.minusHours(1), now.plusMinutes(1));
        assertThat(totals.getTotalEvents()).isEqualTo(4);
        assertThat(totals.getPageViews()).isEqualTo(2);
        assertThat(totals.getUniqueVisitors()).isEqualTo(2);
        assertThat(totals.getSessions()).isEqualTo(2);
        assertThat(totals.getGithubClicks()).isEqualTo(1);
        assertThat(repository.topPages(now.minusHours(1), now.plusMinutes(1), 5))
                .extracting(AnalyticsEventRepository.MetricCountView::getLabel)
                .contains("/", "/projects");
        assertThat(repository.aggregateDaily(now.minusHours(1), now.plusMinutes(1))).isNotEmpty();
    }

    private static AnalyticsEvent event(String type, String path, String visitor, String session, OffsetDateTime at) {
        return AnalyticsEvent.builder()
                .eventType(type)
                .pagePath(path)
                .visitorIdHash(visitor)
                .sessionIdHash(session)
                .createdAt(at)
                .build();
    }
}
