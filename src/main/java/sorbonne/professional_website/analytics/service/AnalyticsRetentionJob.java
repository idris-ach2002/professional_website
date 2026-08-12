package sorbonne.professional_website.analytics.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.analytics.repository.AnalyticsEventRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class AnalyticsRetentionJob {

    private final AnalyticsEventRepository repository;
    private final MeterRegistry meterRegistry;
    private final int retentionDays;

    public AnalyticsRetentionJob(
            AnalyticsEventRepository repository,
            MeterRegistry meterRegistry,
            @Value("${app.analytics.retention-days:365}") int retentionDays
    ) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.retentionDays = Math.max(30, retentionDays);
    }

    @Scheduled(cron = "${app.analytics.retention-cron:0 15 3 * * *}", zone = "UTC")
    @Transactional
    public int purgeExpiredEvents() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            meterRegistry.counter("portfolio.analytics.retention.deleted").increment(deleted);
        }
        return deleted;
    }
}
