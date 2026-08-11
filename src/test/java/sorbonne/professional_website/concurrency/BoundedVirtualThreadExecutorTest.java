package sorbonne.professional_website.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedVirtualThreadExecutorTest {

    @Test
    void rejectsFanOutBeyondConfiguredConcurrencyAndReleasesCapacity() throws Exception {
        try (BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor(1)) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            executor.execute(() -> {
                entered.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(executor.activeCount()).isEqualTo(1);
            assertThatThrownBy(() -> executor.execute(() -> { }))
                    .isInstanceOf(RejectedExecutionException.class);
            release.countDown();
            for (int attempt = 0; attempt < 20 && executor.availablePermits() == 0; attempt++) {
                Thread.sleep(10);
            }
            assertThat(executor.availablePermits()).isEqualTo(1);
        }
    }
}
