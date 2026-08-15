package sorbonne.professional_website.engineering.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RuntimePerformanceSampleResponse(
        UUID id,
        String buildId,
        String runtimeProfile,
        String memoryState,
        Double fps,
        Double frameP95Ms,
        int longTaskCount,
        Double workerLatencyMs,
        Double apiLatencyMs,
        int activeResources,
        OffsetDateTime recordedAt
) { }
