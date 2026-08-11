package sorbonne.professional_website.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import sorbonne.professional_website.concurrency.BoundedVirtualThreadExecutor;

@Component
public class ExecutorMetricsBinder {

    public ExecutorMetricsBinder(
            MeterRegistry registry,
            @Qualifier("cpuExecutor") ThreadPoolTaskExecutor cpu,
            @Qualifier("ioExecutor") ThreadPoolTaskExecutor io,
            @Qualifier("maintenanceExecutor") ThreadPoolTaskExecutor maintenance,
            @Qualifier("virtualIoExecutor") BoundedVirtualThreadExecutor virtualIo
    ) {
        bind(registry, "cpu", cpu);
        bind(registry, "io", io);
        bind(registry, "maintenance", maintenance);
        Gauge.builder("portfolio.executor.virtual.active", virtualIo, BoundedVirtualThreadExecutor::activeCount)
                .tag("pool", "virtual-io")
                .register(registry);
        Gauge.builder("portfolio.executor.virtual.available", virtualIo, BoundedVirtualThreadExecutor::availablePermits)
                .tag("pool", "virtual-io")
                .register(registry);
    }

    private static void bind(MeterRegistry registry, String name, ThreadPoolTaskExecutor executor) {
        Gauge.builder("portfolio.executor.active", executor, ThreadPoolTaskExecutor::getActiveCount)
                .tag("pool", name)
                .register(registry);
        Gauge.builder("portfolio.executor.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize)
                .tag("pool", name)
                .register(registry);
        Gauge.builder("portfolio.executor.queue.size", executor,
                        value -> value.getThreadPoolExecutor().getQueue().size())
                .tag("pool", name)
                .register(registry);
        Gauge.builder("portfolio.executor.queue.remaining", executor,
                        value -> value.getThreadPoolExecutor().getQueue().remainingCapacity())
                .tag("pool", name)
                .register(registry);
    }
}
