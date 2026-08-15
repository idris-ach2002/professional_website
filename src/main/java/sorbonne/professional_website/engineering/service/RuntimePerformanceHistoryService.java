package sorbonne.professional_website.engineering.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.engineering.dto.PerformanceHistoryResponse;
import sorbonne.professional_website.engineering.dto.RuntimePerformanceSampleRequest;
import sorbonne.professional_website.engineering.dto.RuntimePerformanceSampleResponse;
import sorbonne.professional_website.engineering.entity.RuntimePerformanceSample;
import sorbonne.professional_website.engineering.repository.RuntimePerformanceSampleRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RuntimePerformanceHistoryService {

    private static final int MAX_HISTORY_SAMPLES = 400;
    private final RuntimePerformanceSampleRepository repository;

    public RuntimePerformanceHistoryService(RuntimePerformanceSampleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RuntimePerformanceSampleResponse record(RuntimePerformanceSampleRequest request) {
        RuntimePerformanceSample sample = RuntimePerformanceSample.builder()
                .buildId(request.buildId().trim())
                .runtimeProfile(normalizeState(request.runtimeProfile(), "unknown"))
                .memoryState(normalizeState(request.memoryState(), "normal"))
                .fps(request.fps())
                .frameP95Ms(request.frameP95Ms())
                .longTaskCount(request.longTaskCount() == null ? 0 : request.longTaskCount())
                .workerLatencyMs(request.workerLatencyMs())
                .apiLatencyMs(request.apiLatencyMs())
                .activeResources(request.activeResources() == null ? 0 : request.activeResources())
                .build();
        return toResponse(repository.save(sample));
    }

    @Transactional(readOnly = true)
    public PerformanceHistoryResponse history(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 200));
        List<RuntimePerformanceSample> samples = repository.findAllByOrderByRecordedAtDesc(
                PageRequest.of(0, Math.max(limit, MAX_HISTORY_SAMPLES))
        );
        Map<String, Accumulator> accumulators = new LinkedHashMap<>();
        for (RuntimePerformanceSample sample : samples) {
            accumulators.computeIfAbsent(sample.getBuildId(), ignored -> new Accumulator()).add(sample);
        }
        List<PerformanceHistoryResponse.BuildSummary> builds = accumulators.entrySet().stream()
                .limit(12)
                .map(entry -> entry.getValue().summary(entry.getKey()))
                .toList();
        return new PerformanceHistoryResponse(
                builds,
                samples.stream().limit(limit).map(this::toResponse).toList()
        );
    }

    private String normalizeState(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private RuntimePerformanceSampleResponse toResponse(RuntimePerformanceSample sample) {
        return new RuntimePerformanceSampleResponse(
                sample.getId(), sample.getBuildId(), sample.getRuntimeProfile(), sample.getMemoryState(),
                sample.getFps(), sample.getFrameP95Ms(), sample.getLongTaskCount(),
                sample.getWorkerLatencyMs(), sample.getApiLatencyMs(), sample.getActiveResources(),
                sample.getRecordedAt()
        );
    }

    private static final class Accumulator {
        private long count;
        private double fps;
        private int fpsCount;
        private double frameP95;
        private int frameCount;
        private double workerLatency;
        private int workerCount;
        private double apiLatency;
        private int apiCount;
        private int maxResources;
        private java.time.OffsetDateTime lastRecordedAt;

        void add(RuntimePerformanceSample sample) {
            count++;
            if (sample.getFps() != null) { fps += sample.getFps(); fpsCount++; }
            if (sample.getFrameP95Ms() != null) { frameP95 += sample.getFrameP95Ms(); frameCount++; }
            if (sample.getWorkerLatencyMs() != null) { workerLatency += sample.getWorkerLatencyMs(); workerCount++; }
            if (sample.getApiLatencyMs() != null) { apiLatency += sample.getApiLatencyMs(); apiCount++; }
            maxResources = Math.max(maxResources, sample.getActiveResources());
            if (lastRecordedAt == null || sample.getRecordedAt().isAfter(lastRecordedAt)) lastRecordedAt = sample.getRecordedAt();
        }

        PerformanceHistoryResponse.BuildSummary summary(String buildId) {
            return new PerformanceHistoryResponse.BuildSummary(
                    buildId,
                    count,
                    average(fps, fpsCount),
                    average(frameP95, frameCount),
                    average(workerLatency, workerCount),
                    average(apiLatency, apiCount),
                    maxResources,
                    lastRecordedAt
            );
        }

        private static double average(double total, int count) {
            return count == 0 ? 0 : Math.round((total / count) * 100.0) / 100.0;
        }
    }
}
