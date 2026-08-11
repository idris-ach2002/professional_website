package sorbonne.professional_website.concurrency;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Explicit, bounded parallel mapping for immutable/non-JPA inputs.
 * Never pass managed Hibernate entities or lazy collections into this helper.
 */
@Component
public class ParallelWork {

    private final Executor cpuExecutor;

    public ParallelWork(@Qualifier("cpuExecutor") Executor cpuExecutor) {
        this.cpuExecutor = cpuExecutor;
    }

    public <T, R> List<R> mapCpuBounded(List<T> inputs, Function<T, R> mapper, Duration timeout) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        Duration safeTimeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? Duration.ofSeconds(5)
                : timeout;

        List<CompletableFuture<R>> futures = new ArrayList<>(inputs.size());
        for (T input : List.copyOf(inputs)) {
            futures.add(CompletableFuture.supplyAsync(() -> mapper.apply(input), cpuExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(safeTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .join();
            return futures.stream().map(CompletableFuture::join).toList();
        } catch (RuntimeException exception) {
            futures.forEach(future -> future.cancel(true));
            throw exception;
        }
    }
}
