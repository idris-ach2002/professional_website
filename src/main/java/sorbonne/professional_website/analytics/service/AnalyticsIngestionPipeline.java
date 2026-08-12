package sorbonne.professional_website.analytics.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;
import sorbonne.professional_website.concurrency.BackendConcurrencyProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded analytics pipeline. Request threads only normalize/hash/enqueue.
 * A single transactional batch writer drains the queue, avoiding one SQL
 * transaction per event and providing explicit backpressure.
 */
@Component
public class AnalyticsIngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsIngestionPipeline.class);

    private final ArrayBlockingQueue<AnalyticsEvent> queue;
    private final int batchSize;
    private final AnalyticsBatchWriter writer;
    private final MeterRegistry meterRegistry;
    private final Executor maintenanceExecutor;
    private final ReentrantLock flushLock = new ReentrantLock();
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    public AnalyticsIngestionPipeline(
            BackendConcurrencyProperties properties,
            AnalyticsBatchWriter writer,
            MeterRegistry meterRegistry,
            @Qualifier("maintenanceExecutor") Executor maintenanceExecutor
    ) {
        this.queue = new ArrayBlockingQueue<>(properties.analyticsQueueCapacity());
        this.batchSize = properties.analyticsBatchSize();
        this.writer = writer;
        this.meterRegistry = meterRegistry;
        this.maintenanceExecutor = maintenanceExecutor;
        meterRegistry.gauge("portfolio.analytics.queue.size", queue, ArrayBlockingQueue::size);
        meterRegistry.gauge("portfolio.analytics.queue.remaining", queue, ArrayBlockingQueue::remainingCapacity);
    }

    public boolean offer(AnalyticsEvent event) {
        boolean accepted = event != null && queue.offer(event);
        meterRegistry.counter(accepted ? "portfolio.analytics.accepted" : "portfolio.analytics.rejected").increment();
        if (accepted && queue.size() >= batchSize) triggerAsyncFlush();
        return accepted;
    }


    private void triggerAsyncFlush() {
        if (!flushScheduled.compareAndSet(false, true)) return;
        try {
            maintenanceExecutor.execute(() -> {
                try {
                    flush();
                } finally {
                    flushScheduled.set(false);
                    if (queue.size() >= batchSize) triggerAsyncFlush();
                }
            });
        } catch (RejectedExecutionException exception) {
            flushScheduled.set(false);
            meterRegistry.counter("portfolio.analytics.flush.rejected").increment();
            // The bounded queue keeps the events safe until the scheduled flush.
        }
    }

    @Scheduled(fixedDelayString = "${app.concurrency.analytics-flush-interval-ms:750}")
    public void scheduledFlush() {
        triggerAsyncFlush();
    }

    public void flush() {
        if (queue.isEmpty() || !flushLock.tryLock()) return;
        try {
            flushOneBatch();
        } finally {
            flushLock.unlock();
        }
    }

    private void flushOneBatch() {
        List<AnalyticsEvent> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        if (batch.isEmpty()) return;
        try {
            writer.write(List.copyOf(batch));
            meterRegistry.counter("portfolio.analytics.batch.success").increment(batch.size());
        } catch (RuntimeException exception) {
            meterRegistry.counter("portfolio.analytics.batch.failure").increment(batch.size());
            // Best-effort requeue. Analytics is non-critical; never exhaust heap
            // if PostgreSQL is unavailable.
            for (AnalyticsEvent event : batch) {
                if (!queue.offer(event)) break;
            }
            log.warn("Analytics batch write failed; {} events were requeued when capacity allowed", batch.size(), exception);
        }
    }

    int queuedEvents() {
        return queue.size();
    }

    @PreDestroy
    void shutdownFlush() {
        flushLock.lock();
        try {
            // Wait for an in-flight batch, then drain a bounded number of batches
            // synchronously. A persistent database outage must not hang shutdown.
            for (int attempt = 0; attempt < 4 && !queue.isEmpty(); attempt++) {
                int before = queue.size();
                flushOneBatch();
                if (queue.size() >= before) break;
            }
        } finally {
            flushLock.unlock();
        }
    }
}
