package sorbonne.professional_website.concurrency;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.concurrency")
public record BackendConcurrencyProperties(
        int virtualIoMaxConcurrency,
        int maintenanceCoreSize,
        int maintenanceMaxSize,
        int maintenanceQueueCapacity,
        int analyticsQueueCapacity,
        int analyticsBatchSize,
        long analyticsFlushIntervalMs
) {
    public BackendConcurrencyProperties {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        virtualIoMaxConcurrency = positiveOr(virtualIoMaxConcurrency, Math.max(8, Math.min(32, processors * 2)));
        maintenanceCoreSize = positiveOr(maintenanceCoreSize, 1);
        maintenanceMaxSize = Math.max(maintenanceCoreSize, positiveOr(maintenanceMaxSize, 2));
        maintenanceQueueCapacity = positiveOr(maintenanceQueueCapacity, 64);
        analyticsQueueCapacity = positiveOr(analyticsQueueCapacity, 512);
        analyticsBatchSize = positiveOr(analyticsBatchSize, 32);
        analyticsFlushIntervalMs = analyticsFlushIntervalMs > 0 ? analyticsFlushIntervalMs : 750L;
    }

    private static int positiveOr(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
