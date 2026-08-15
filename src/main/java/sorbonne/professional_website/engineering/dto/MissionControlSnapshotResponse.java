package sorbonne.professional_website.engineering.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record MissionControlSnapshotResponse(
        OffsetDateTime generatedAt,
        String status,
        DatabaseStatus database,
        SystemTelemetry system,
        List<CacheStatus> caches,
        AnalyticsQueueStatus analyticsQueue,
        Map<String, Long> jobs,
        Map<String, Long> outbox,
        Map<String, Long> publications,
        List<SystemEvent> recentEvents,
        List<ArchitectureNode> architecture,
        List<ArchitectureLink> links
) {
    public record DatabaseStatus(boolean reachable, long latencyMs, String engine) { }

    public record CacheStatus(
            String name,
            long hits,
            long misses,
            double hitRate,
            long estimatedSize
    ) { }

    public record AnalyticsQueueStatus(
            int queued,
            int capacity,
            int remaining,
            double saturationPercent
    ) { }

    public record SystemEvent(
            String id,
            String type,
            String state,
            OffsetDateTime occurredAt
    ) { }

    public record SystemTelemetry(
            OperatingSystemDetails operatingSystem,
            CpuTelemetry cpu,
            MemoryTelemetry memory,
            StorageTelemetry storage,
            JavaRuntimeTelemetry javaRuntime
    ) { }

    public record OperatingSystemDetails(
            String name,
            String version,
            String architecture,
            int logicalProcessors,
            String cpuModel
    ) { }

    public record CpuTelemetry(
            double systemLoadPercent,
            double processLoadPercent,
            double loadAverage,
            long processCpuTimeNanos
    ) { }

    public record MemoryTelemetry(
            long physicalTotalBytes,
            long physicalUsedBytes,
            long physicalFreeBytes,
            long swapTotalBytes,
            long swapUsedBytes,
            long heapUsedBytes,
            long heapCommittedBytes,
            long heapMaxBytes,
            long nonHeapUsedBytes
    ) { }

    public record StorageTelemetry(long totalBytes, long usedBytes, long usableBytes, String fileSystem) { }

    public record JavaRuntimeTelemetry(
            String version,
            String vendor,
            String virtualMachine,
            long uptimeMs,
            long startedAtEpochMs
    ) { }

    public record ArchitectureNode(
            String id,
            String label,
            String layer,
            String status,
            String technology,
            double activity
    ) { }

    public record ArchitectureLink(
            String source,
            String target,
            String channel,
            boolean active,
            double activity
    ) { }
}
