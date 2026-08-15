package sorbonne.professional_website.analytics.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;
import sorbonne.professional_website.concurrency.BackendConcurrencyProperties;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AnalyticsIngestionPipelineTest {

    @Test
    void requestThreadOnlyEnqueuesAndBatchWriteRunsOnExecutor() {
        AnalyticsBatchWriter writer = mock(AnalyticsBatchWriter.class);
        CapturingExecutor executor = new CapturingExecutor();
        AnalyticsIngestionPipeline pipeline = new AnalyticsIngestionPipeline(
                properties(4, 2),
                writer,
                new SimpleMeterRegistry(),
                executor
        );

        AnalyticsEvent first = AnalyticsEvent.builder().eventType("page_view").build();
        AnalyticsEvent second = AnalyticsEvent.builder().eventType("page_view").build();

        assertThat(pipeline.offer(first)).isTrue();
        assertThat(pipeline.offer(second)).isTrue();
        verify(writer, never()).write(org.mockito.ArgumentMatchers.anyList());
        assertThat(executor.pending()).isEqualTo(1);

        executor.runNext();

        verify(writer).write(List.of(first, second));
        assertThat(pipeline.queuedEvents()).isZero();
    }

    @Test
    void shutdownDrainsQueuedEventsSynchronouslyWhenAsyncExecutionWasRejected() {
        AnalyticsBatchWriter writer = mock(AnalyticsBatchWriter.class);
        Executor rejectingExecutor = command -> { throw new java.util.concurrent.RejectedExecutionException("busy"); };
        AnalyticsIngestionPipeline pipeline = new AnalyticsIngestionPipeline(
                properties(4, 2),
                writer,
                new SimpleMeterRegistry(),
                rejectingExecutor
        );

        AnalyticsEvent first = AnalyticsEvent.builder().eventType("a").build();
        AnalyticsEvent second = AnalyticsEvent.builder().eventType("b").build();
        pipeline.offer(first);
        pipeline.offer(second);

        pipeline.shutdownFlush();

        verify(writer).write(List.of(first, second));
        assertThat(pipeline.queuedEvents()).isZero();
    }

    @Test
    void boundedQueueAppliesBackpressureInsteadOfGrowingWithoutLimit() {
        AnalyticsBatchWriter writer = mock(AnalyticsBatchWriter.class);
        Executor rejectingExecutor = command -> { throw new java.util.concurrent.RejectedExecutionException("busy"); };
        AnalyticsIngestionPipeline pipeline = new AnalyticsIngestionPipeline(
                properties(2, 2),
                writer,
                new SimpleMeterRegistry(),
                rejectingExecutor
        );

        assertThat(pipeline.offer(AnalyticsEvent.builder().eventType("a").build())).isTrue();
        assertThat(pipeline.offer(AnalyticsEvent.builder().eventType("b").build())).isTrue();
        assertThat(pipeline.offer(AnalyticsEvent.builder().eventType("c").build())).isFalse();
        assertThat(pipeline.queuedEvents()).isEqualTo(2);
        verify(writer, never()).write(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void exposesBoundedCapacityAndPaginatedReadOnlySnapshot() {
        AnalyticsBatchWriter writer = mock(AnalyticsBatchWriter.class);
        Executor rejectingExecutor = command -> { throw new java.util.concurrent.RejectedExecutionException("busy"); };
        AnalyticsIngestionPipeline pipeline = new AnalyticsIngestionPipeline(
                properties(4, 4),
                writer,
                new SimpleMeterRegistry(),
                rejectingExecutor
        );

        AnalyticsEvent first = AnalyticsEvent.builder().eventType("first").build();
        AnalyticsEvent second = AnalyticsEvent.builder().eventType("second").build();
        AnalyticsEvent third = AnalyticsEvent.builder().eventType("third").build();
        pipeline.offer(first);
        pipeline.offer(second);
        pipeline.offer(third);

        assertThat(pipeline.capacity()).isEqualTo(4);
        assertThat(pipeline.remainingCapacity()).isEqualTo(1);
        assertThat(pipeline.snapshotPage(0, 2)).containsExactly(first, second);
        assertThat(pipeline.snapshotPage(1, 2)).containsExactly(third);
        assertThat(pipeline.queuedEvents()).isEqualTo(3);
        verify(writer, never()).write(org.mockito.ArgumentMatchers.anyList());
    }

    private static BackendConcurrencyProperties properties(int queueCapacity, int batchSize) {
        return new BackendConcurrencyProperties(
                8,
                1, 1, 8,
                queueCapacity, batchSize, 500
        );
    }

    private static final class CapturingExecutor implements Executor {
        private final Queue<Runnable> commands = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            commands.add(command);
        }

        int pending() {
            return commands.size();
        }

        void runNext() {
            commands.remove().run();
        }
    }
}
