package sorbonne.professional_website.engineering.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PerformanceHistoryResponse(
        List<BuildSummary> builds,
        List<RuntimePerformanceSampleResponse> recentSamples
) {
    public record BuildSummary(
            String buildId,
            long sampleCount,
            double averageFps,
            double averageFrameP95Ms,
            double averageWorkerLatencyMs,
            double averageApiLatencyMs,
            int maximumActiveResources,
            OffsetDateTime lastRecordedAt
    ) { }
}
