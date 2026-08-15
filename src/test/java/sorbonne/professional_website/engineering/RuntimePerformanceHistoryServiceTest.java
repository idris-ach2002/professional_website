package sorbonne.professional_website.engineering;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import sorbonne.professional_website.engineering.dto.RuntimePerformanceSampleRequest;
import sorbonne.professional_website.engineering.entity.RuntimePerformanceSample;
import sorbonne.professional_website.engineering.repository.RuntimePerformanceSampleRepository;
import sorbonne.professional_website.engineering.service.RuntimePerformanceHistoryService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimePerformanceHistoryServiceTest {

    @Mock
    private RuntimePerformanceSampleRepository repository;
    private RuntimePerformanceHistoryService service;

    @BeforeEach
    void setUp() {
        service = new RuntimePerformanceHistoryService(repository);
    }

    @Test
    void recordNormalizesRuntimeStatesAndPersistsRealMetrics() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(new RuntimePerformanceSampleRequest(
                " build-42 ", "Balanced Mode", "Memory Pressure", 119.8, 8.7,
                2, 3.2, 24.5, 18
        ));

        assertThat(response.buildId()).isEqualTo("build-42");
        assertThat(response.runtimeProfile()).isEqualTo("balanced-mode");
        assertThat(response.memoryState()).isEqualTo("memory-pressure");
        assertThat(response.fps()).isEqualTo(119.8);
        assertThat(response.activeResources()).isEqualTo(18);
    }

    @Test
    void historyAggregatesMetricsByBuildAndKeepsNewestBuildFirst() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-13T12:00:00Z");
        when(repository.findAllByOrderByRecordedAtDesc(any(Pageable.class))).thenReturn(List.of(
                sample("build-new", 120, 8, 4, 20, now),
                sample("build-new", 100, 12, 6, 30, now.minusMinutes(1)),
                sample("build-old", 60, 18, 10, 40, now.minusDays(1))
        ));

        var history = service.history(20);

        assertThat(history.builds()).hasSize(2);
        assertThat(history.builds().getFirst().buildId()).isEqualTo("build-new");
        assertThat(history.builds().getFirst().averageFps()).isEqualTo(110);
        assertThat(history.builds().getFirst().averageFrameP95Ms()).isEqualTo(10);
        assertThat(history.builds().getFirst().maximumActiveResources()).isEqualTo(30);
        assertThat(history.recentSamples()).hasSize(3);
    }

    private RuntimePerformanceSample sample(String build, double fps, double p95, double worker, int resources, OffsetDateTime at) {
        return RuntimePerformanceSample.builder()
                .buildId(build).runtimeProfile("balanced").memoryState("normal")
                .fps(fps).frameP95Ms(p95).workerLatencyMs(worker).apiLatencyMs(12.0)
                .activeResources(resources).recordedAt(at).build();
    }
}
