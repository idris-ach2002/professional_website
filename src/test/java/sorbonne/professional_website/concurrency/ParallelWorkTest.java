package sorbonne.professional_website.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelWorkTest {

    @Test
    void mapsImmutableInputsAndPreservesEncounterOrder() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            ParallelWork work = new ParallelWork(executor);
            List<Integer> result = work.mapCpuBounded(List.of(1, 2, 3, 4), value -> value * value, Duration.ofSeconds(1));
            assertThat(result).containsExactly(1, 4, 9, 16);
        } finally {
            executor.shutdownNow();
        }
    }
}
