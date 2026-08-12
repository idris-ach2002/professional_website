package sorbonne.professional_website.analytics.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import sorbonne.professional_website.analytics.repository.AnalyticsEventRepository;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsRetentionJobTest {

    @Test
    void purgesExpiredEventsAndPublishesDeletionMetric() {
        AnalyticsEventRepository repository = mock(AnalyticsEventRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(repository.deleteOlderThan(any(OffsetDateTime.class))).thenReturn(7);

        AnalyticsRetentionJob job = new AnalyticsRetentionJob(repository, registry, 90);

        assertThat(job.purgeExpiredEvents()).isEqualTo(7);
        verify(repository).deleteOlderThan(any(OffsetDateTime.class));
        assertThat(registry.counter("portfolio.analytics.retention.deleted").count()).isEqualTo(7.0);
    }
}
