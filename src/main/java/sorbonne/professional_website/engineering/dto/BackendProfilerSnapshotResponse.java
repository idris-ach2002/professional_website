package sorbonne.professional_website.engineering.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record BackendProfilerSnapshotResponse(
        OffsetDateTime sampledAt,
        long activeRequests,
        SystemCost system,
        Summary summary,
        List<RouteProfile> routes
) {
    public record SystemCost(
            double systemCpuPercent,
            double processCpuPercent,
            long heapUsedBytes,
            long heapMaxBytes
    ) {}

    public record Summary(
            int routeCount,
            long observedRouteCount,
            long requestCount,
            long errorCount,
            double averageMs,
            double p95Ms,
            double p99Ms
    ) {}

    public record RouteProfile(
            String method,
            String route,
            String controller,
            String handler,
            boolean observed,
            boolean benchmarkable,
            String benchmarkPath,
            long sampleCount,
            long errorCount,
            double errorRatePercent,
            double averageMs,
            double p50Ms,
            double p95Ms,
            double p99Ms,
            double maxMs,
            double lastMs,
            double averageCpuMs,
            double p95CpuMs,
            long averageAllocatedBytes,
            long p95AllocatedBytes,
            int lastStatus,
            OffsetDateTime lastObservedAt
    ) {}
}
