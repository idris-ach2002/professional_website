package sorbonne.professional_website.concurrency;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 21 virtual-thread lane with an explicit concurrency ceiling.
 * Virtual threads remove the cost of one platform thread per blocking task,
 * while the semaphore prevents accidental fan-out from saturating external
 * services or the database connection pool.
 */
public final class BoundedVirtualThreadExecutor implements Executor, AutoCloseable {

    private final int maxConcurrency;
    private final Semaphore permits;
    private final ExecutorService delegate;
    private final AtomicInteger active = new AtomicInteger();

    public BoundedVirtualThreadExecutor(int maxConcurrency) {
        this.maxConcurrency = Math.max(1, maxConcurrency);
        this.permits = new Semaphore(this.maxConcurrency);
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        if (!permits.tryAcquire()) {
            throw new RejectedExecutionException("Virtual I/O concurrency limit reached: " + maxConcurrency);
        }
        try {
            delegate.execute(() -> {
                active.incrementAndGet();
                try {
                    command.run();
                } finally {
                    active.decrementAndGet();
                    permits.release();
                }
            });
        } catch (RuntimeException exception) {
            permits.release();
            throw exception;
        }
    }

    public int activeCount() {
        return active.get();
    }

    public int availablePermits() {
        return permits.availablePermits();
    }

    public int maxConcurrency() {
        return maxConcurrency;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
