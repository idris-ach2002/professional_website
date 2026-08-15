package sorbonne.professional_website.engineering.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.analytics.service.AnalyticsIngestionPipeline;
import sorbonne.professional_website.cache.PublicPortfolioCacheConfig;
import sorbonne.professional_website.engineering.dto.EngineeringQueuePageResponse;
import sorbonne.professional_website.engineering.dto.MissionControlSnapshotResponse;
import sorbonne.professional_website.events.OutboxEvent;
import sorbonne.professional_website.events.OutboxEventRepository;
import sorbonne.professional_website.events.OutboxStatus;
import sorbonne.professional_website.jobs.BackgroundJob;
import sorbonne.professional_website.jobs.BackgroundJobRepository;
import sorbonne.professional_website.jobs.BackgroundJobStatus;
import sorbonne.professional_website.publication.PublicationStatus;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class MissionControlService {

    private static final List<String> CACHE_NAMES = List.of(
            PublicPortfolioCacheConfig.WEBSITE_CACHE,
            PublicPortfolioCacheConfig.WEBSITE_LIST_CACHE,
            PublicPortfolioCacheConfig.PROJECT_CACHE,
            PublicPortfolioCacheConfig.SEO_CACHE
    );

    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final AnalyticsIngestionPipeline analyticsPipeline;
    private final BackgroundJobRepository jobRepository;
    private final OutboxEventRepository outboxRepository;
    private final WebsiteVersionRepository versionRepository;

    public MissionControlService(
            DataSource dataSource,
            CacheManager cacheManager,
            AnalyticsIngestionPipeline analyticsPipeline,
            BackgroundJobRepository jobRepository,
            OutboxEventRepository outboxRepository,
            WebsiteVersionRepository versionRepository
    ) {
        this.dataSource = dataSource;
        this.cacheManager = cacheManager;
        this.analyticsPipeline = analyticsPipeline;
        this.jobRepository = jobRepository;
        this.outboxRepository = outboxRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public MissionControlSnapshotResponse snapshot() {
        MissionControlSnapshotResponse.DatabaseStatus database = databaseStatus();
        MissionControlSnapshotResponse.SystemTelemetry system = systemTelemetry();
        List<MissionControlSnapshotResponse.CacheStatus> caches = CACHE_NAMES.stream().map(this::cacheStatus).toList();
        MissionControlSnapshotResponse.AnalyticsQueueStatus analyticsQueue = analyticsQueueStatus();
        Map<String, Long> jobs = enumCounts(BackgroundJobStatus.values(), jobRepository::countByStatus);
        Map<String, Long> outbox = enumCounts(OutboxStatus.values(), outboxRepository::countByStatus);
        Map<String, Long> publications = enumCounts(PublicationStatus.values(), versionRepository::countByPublicationStatus);
        List<MissionControlSnapshotResponse.SystemEvent> recentEvents = outboxRepository
                .findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::eventResponse)
                .toList();

        String globalStatus = database.reachable() && outbox.getOrDefault("DEAD", 0L) == 0 ? "operational" : "degraded";

        return new MissionControlSnapshotResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                globalStatus,
                database,
                system,
                caches,
                analyticsQueue,
                jobs,
                outbox,
                publications,
                recentEvents,
                architecture(database, caches, jobs, outbox),
                links(database, caches, jobs, outbox)
        );
    }

    private MissionControlSnapshotResponse.AnalyticsQueueStatus analyticsQueueStatus() {
        int queued = analyticsPipeline.queuedEvents();
        int capacity = Math.max(1, analyticsPipeline.capacity());
        return new MissionControlSnapshotResponse.AnalyticsQueueStatus(
                queued,
                capacity,
                analyticsPipeline.remainingCapacity(),
                round((double) queued / capacity * 100.0)
        );
    }

    @Transactional(readOnly = true)
    public EngineeringQueuePageResponse queuePage(String requestedKind, int requestedPage, int requestedSize) {
        String kind = requestedKind == null ? "analytics" : requestedKind.trim().toLowerCase();
        int page = Math.max(0, requestedPage);
        int size = Math.max(5, Math.min(25, requestedSize));

        if ("jobs".equals(kind)) {
            var result = jobRepository.findByStatusIn(
                    List.of(BackgroundJobStatus.QUEUED, BackgroundJobStatus.RUNNING, BackgroundJobStatus.RETRYING),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            return new EngineeringQueuePageResponse(
                    "jobs", page, size, result.getTotalElements(), result.getTotalPages(), null,
                    Math.toIntExact(Math.min(Integer.MAX_VALUE, result.getTotalElements())), null,
                    result.getContent().stream().map(this::jobQueueItem).toList()
            );
        }
        if ("outbox".equals(kind)) {
            var result = outboxRepository.findByStatusIn(
                    List.of(OutboxStatus.PENDING, OutboxStatus.PROCESSING),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            return new EngineeringQueuePageResponse(
                    "outbox", page, size, result.getTotalElements(), result.getTotalPages(), null,
                    Math.toIntExact(Math.min(Integer.MAX_VALUE, result.getTotalElements())), null,
                    result.getContent().stream().map(this::outboxQueueItem).toList()
            );
        }

        int queued = analyticsPipeline.queuedEvents();
        int capacity = Math.max(1, analyticsPipeline.capacity());
        int totalPages = queued == 0 ? 0 : (int) Math.ceil((double) queued / size);
        return new EngineeringQueuePageResponse(
                "analytics", page, size, queued, totalPages, capacity, queued,
                round((double) queued / capacity * 100.0),
                analyticsPipeline.snapshotPage(page, size).stream()
                        .map(event -> new EngineeringQueuePageResponse.QueueItem(
                                event.getId() == null ? "pending" : event.getId().toString(),
                                event.getEventType(),
                                "QUEUED",
                                null, null, null, null,
                                event.getCreatedAt(), null, null
                        ))
                        .toList()
        );
    }

    private EngineeringQueuePageResponse.QueueItem jobQueueItem(BackgroundJob job) {
        return new EngineeringQueuePageResponse.QueueItem(
                job.getId(), job.getType().name(), job.getStatus().name(), job.getProgress(), job.getPriority(),
                job.getAttempts(), job.getMaxAttempts(),
                sorbonne.professional_website.time.PlatformTime.asUtcOffset(job.getCreatedAt()),
                sorbonne.professional_website.time.PlatformTime.asUtcOffset(job.getExecuteAfter()),
                sorbonne.professional_website.time.PlatformTime.asUtcOffset(job.getCompletedAt())
        );
    }

    private EngineeringQueuePageResponse.QueueItem outboxQueueItem(OutboxEvent event) {
        return new EngineeringQueuePageResponse.QueueItem(
                event.getId(), event.getEventType(), event.getStatus().name(), null, null,
                event.getAttempts(), null,
                sorbonne.professional_website.time.PlatformTime.asUtcOffset(event.getCreatedAt()),
                sorbonne.professional_website.time.PlatformTime.asUtcOffset(event.getNextAttemptAt()),
                sorbonne.professional_website.time.PlatformTime.asUtcOffset(event.getDispatchedAt())
        );
    }

    private MissionControlSnapshotResponse.SystemTelemetry systemTelemetry() {
        java.lang.management.OperatingSystemMXBean baseOperatingSystem = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean operatingSystem = baseOperatingSystem instanceof com.sun.management.OperatingSystemMXBean extended
                ? extended
                : null;
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        long physicalTotal = operatingSystem == null ? -1 : operatingSystem.getTotalMemorySize();
        long physicalFree = operatingSystem == null ? -1 : operatingSystem.getFreeMemorySize();
        long swapTotal = operatingSystem == null ? -1 : operatingSystem.getTotalSwapSpaceSize();
        long swapFree = operatingSystem == null ? -1 : operatingSystem.getFreeSwapSpaceSize();

        return new MissionControlSnapshotResponse.SystemTelemetry(
                new MissionControlSnapshotResponse.OperatingSystemDetails(
                        baseOperatingSystem.getName(),
                        baseOperatingSystem.getVersion(),
                        baseOperatingSystem.getArch(),
                        baseOperatingSystem.getAvailableProcessors(),
                        cpuModel()
                ),
                new MissionControlSnapshotResponse.CpuTelemetry(
                        percent(operatingSystem == null ? -1 : operatingSystem.getCpuLoad()),
                        percent(operatingSystem == null ? -1 : operatingSystem.getProcessCpuLoad()),
                        sanitize(baseOperatingSystem.getSystemLoadAverage()),
                        operatingSystem == null ? -1 : operatingSystem.getProcessCpuTime()
                ),
                new MissionControlSnapshotResponse.MemoryTelemetry(
                        physicalTotal,
                        used(physicalTotal, physicalFree),
                        physicalFree,
                        swapTotal,
                        used(swapTotal, swapFree),
                        heap.getUsed(),
                        heap.getCommitted(),
                        heap.getMax(),
                        nonHeap.getUsed()
                ),
                storageTelemetry(),
                new MissionControlSnapshotResponse.JavaRuntimeTelemetry(
                        System.getProperty("java.version", "unknown"),
                        System.getProperty("java.vendor", "unknown"),
                        System.getProperty("java.vm.name", "unknown"),
                        runtime.getUptime(),
                        runtime.getStartTime()
                )
        );
    }

    private MissionControlSnapshotResponse.StorageTelemetry storageTelemetry() {
        try {
            FileStore store = Files.getFileStore(Path.of(".").toAbsolutePath());
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            return new MissionControlSnapshotResponse.StorageTelemetry(total, used(total, usable), usable, store.type());
        } catch (Exception ignored) {
            return new MissionControlSnapshotResponse.StorageTelemetry(-1, -1, -1, "unavailable");
        }
    }

    private String cpuModel() {
        String environmentModel = System.getenv("PROCESSOR_IDENTIFIER");
        if (environmentModel != null && !environmentModel.isBlank()) return environmentModel.trim();
        Path cpuInfo = Path.of("/proc/cpuinfo");
        if (!Files.isReadable(cpuInfo)) return ManagementFactory.getOperatingSystemMXBean().getArch();
        try (Stream<String> lines = Files.lines(cpuInfo)) {
            return lines
                    .map(String::trim)
                    .filter(line -> line.startsWith("model name") || line.startsWith("Hardware"))
                    .map(line -> line.substring(line.indexOf(':') + 1).trim())
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse(ManagementFactory.getOperatingSystemMXBean().getArch());
        } catch (Exception ignored) {
            return ManagementFactory.getOperatingSystemMXBean().getArch();
        }
    }

    private MissionControlSnapshotResponse.DatabaseStatus databaseStatus() {
        long startedAt = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            String engine = connection.getMetaData().getDatabaseProductName();
            return new MissionControlSnapshotResponse.DatabaseStatus(valid, elapsedMs(startedAt), engine);
        } catch (Exception exception) {
            return new MissionControlSnapshotResponse.DatabaseStatus(false, elapsedMs(startedAt), "unavailable");
        }
    }

    private MissionControlSnapshotResponse.CacheStatus cacheStatus(String name) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(name);
        if (!(springCache instanceof CaffeineCache caffeineCache)) {
            return new MissionControlSnapshotResponse.CacheStatus(name, 0, 0, 0, 0);
        }
        Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        var stats = nativeCache.stats();
        return new MissionControlSnapshotResponse.CacheStatus(
                name,
                stats.hitCount(),
                stats.missCount(),
                round(stats.hitRate()),
                nativeCache.estimatedSize()
        );
    }

    private <E extends Enum<E>> Map<String, Long> enumCounts(E[] values, java.util.function.ToLongFunction<E> counter) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Arrays.stream(values).forEach(value -> counts.put(value.name(), counter.applyAsLong(value)));
        return counts;
    }

    private MissionControlSnapshotResponse.SystemEvent eventResponse(OutboxEvent event) {
        return new MissionControlSnapshotResponse.SystemEvent(
                event.getId(),
                event.getEventType(),
                event.getStatus().name(),
                event.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    private List<MissionControlSnapshotResponse.ArchitectureNode> architecture(
            MissionControlSnapshotResponse.DatabaseStatus database,
            List<MissionControlSnapshotResponse.CacheStatus> caches,
            Map<String, Long> jobs,
            Map<String, Long> outbox
    ) {
        long cacheTraffic = caches.stream().mapToLong(cache -> cache.hits() + cache.misses()).sum();
        long activeJobs = jobs.getOrDefault("RUNNING", 0L) + jobs.getOrDefault("QUEUED", 0L) + jobs.getOrDefault("RETRYING", 0L);
        long activeOutbox = outbox.getOrDefault("PENDING", 0L) + outbox.getOrDefault("PROCESSING", 0L);
        return List.of(
                node("browser", "Browser", "client", "operational", "Web APIs", 1),
                node("react", "React Runtime", "client", "operational", "React 19", 1),
                node("api", "Spring API", "application", "operational", "Spring Boot 4", 1),
                node("cache", "Cache", "data", cacheTraffic > 0 ? "active" : "idle", "Caffeine", normalized(cacheTraffic, 100)),
                node("postgres", "Database", "data", database.reachable() ? "operational" : "degraded", "PostgreSQL", database.reachable() ? normalized(30 - database.latencyMs(), 30) : 0),
                node("outbox", "Event Outbox", "event", activeOutbox > 0 ? "active" : "idle", "Transactional Outbox", normalized(activeOutbox, 12)),
                node("jobs", "Async Jobs", "worker", activeJobs > 0 ? "active" : "idle", "Spring Scheduler", normalized(activeJobs, 8))
        );
    }

    private List<MissionControlSnapshotResponse.ArchitectureLink> links(
            MissionControlSnapshotResponse.DatabaseStatus database,
            List<MissionControlSnapshotResponse.CacheStatus> caches,
            Map<String, Long> jobs,
            Map<String, Long> outbox
    ) {
        long cacheTraffic = caches.stream().mapToLong(cache -> cache.hits() + cache.misses()).sum();
        long activeJobs = jobs.getOrDefault("RUNNING", 0L) + jobs.getOrDefault("QUEUED", 0L) + jobs.getOrDefault("RETRYING", 0L);
        long activeOutbox = outbox.getOrDefault("PENDING", 0L) + outbox.getOrDefault("PROCESSING", 0L);
        return List.of(
                link("browser", "react", "navigation", true, 1),
                link("react", "api", "HTTPS / JSON", true, 1),
                link("api", "cache", "cache lookup", cacheTraffic > 0, normalized(cacheTraffic, 100)),
                link("api", "postgres", "JDBC", database.reachable(), database.reachable() ? normalized(30 - database.latencyMs(), 30) : 0),
                link("postgres", "outbox", "same transaction", activeOutbox > 0, normalized(activeOutbox, 12)),
                link("outbox", "jobs", "dispatch", activeJobs > 0 || activeOutbox > 0, normalized(activeJobs + activeOutbox, 16))
        );
    }

    private MissionControlSnapshotResponse.ArchitectureNode node(
            String id, String label, String layer, String status, String technology, double activity
    ) {
        return new MissionControlSnapshotResponse.ArchitectureNode(id, label, layer, status, technology, activity);
    }

    private MissionControlSnapshotResponse.ArchitectureLink link(
            String source, String target, String channel, boolean active, double activity
    ) {
        return new MissionControlSnapshotResponse.ArchitectureLink(source, target, channel, active, activity);
    }

    private long used(long total, long free) {
        if (total < 0 || free < 0) return -1;
        return Math.max(0, total - free);
    }

    private double percent(double ratio) {
        return ratio < 0 ? -1 : round(Math.min(1, ratio) * 100);
    }

    private double sanitize(double value) {
        return Double.isFinite(value) && value >= 0 ? round(value) : -1;
    }

    private double normalized(long value, long ceiling) {
        return round(Math.max(0, Math.min(1, value / (double) Math.max(1, ceiling))));
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, Math.round((System.nanoTime() - startedAt) / 1_000_000.0));
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
