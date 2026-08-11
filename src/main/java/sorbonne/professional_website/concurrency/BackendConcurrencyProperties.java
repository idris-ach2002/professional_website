package sorbonne.professional_website.concurrency;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.concurrency")
public record BackendConcurrencyProperties(
        int cpuCoreSize,
        int cpuMaxSize,
        int cpuQueueCapacity,
        int ioCoreSize,
        int ioMaxSize,
        int ioQueueCapacity,
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
        cpuCoreSize = positiveOr(cpuCoreSize, Math.max(2, Math.min(8, processors - 1)));
        cpuMaxSize = Math.max(cpuCoreSize, positiveOr(cpuMaxSize, Math.max(cpuCoreSize, Math.min(12, processors))));
        cpuQueueCapacity = positiveOr(cpuQueueCapacity, 128);

        ioCoreSize = positiveOr(ioCoreSize, Math.max(2, Math.min(4, processors / 2)));
        ioMaxSize = Math.max(ioCoreSize, positiveOr(ioMaxSize, Math.max(4, Math.min(12, processors))));
        ioQueueCapacity = positiveOr(ioQueueCapacity, 128);
        virtualIoMaxConcurrency = positiveOr(virtualIoMaxConcurrency, Math.max(8, Math.min(64, processors * 4)));

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
