package sorbonne.professional_website.engineering.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sorbonne.professional_website.analytics.service.AnalyticsRateLimiter;
import sorbonne.professional_website.engineering.dto.EngineeringQueuePageResponse;
import sorbonne.professional_website.engineering.dto.MissionControlSnapshotResponse;
import sorbonne.professional_website.engineering.dto.PerformanceHistoryResponse;
import sorbonne.professional_website.engineering.dto.RuntimePerformanceSampleRequest;
import sorbonne.professional_website.engineering.dto.RuntimePerformanceSampleResponse;
import sorbonne.professional_website.engineering.service.MissionControlService;
import sorbonne.professional_website.engineering.service.RuntimePerformanceHistoryService;

@RestController
@RequestMapping("/api/engineering")
public class EngineeringMissionControlController {

    private final MissionControlService missionControlService;
    private final RuntimePerformanceHistoryService performanceHistoryService;
    private final AnalyticsRateLimiter rateLimiter;

    public EngineeringMissionControlController(
            MissionControlService missionControlService,
            RuntimePerformanceHistoryService performanceHistoryService,
            AnalyticsRateLimiter rateLimiter
    ) {
        this.missionControlService = missionControlService;
        this.performanceHistoryService = performanceHistoryService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/mission-control")
    public ResponseEntity<MissionControlSnapshotResponse> snapshot() {
        long startedAt = System.nanoTime();
        MissionControlSnapshotResponse snapshot = missionControlService.snapshot();
        double springDurationMs = elapsedMs(startedAt);
        String serverTiming = "spring;dur=" + springDurationMs + ";desc=\"MissionControlService\""
                + ", postgres;dur=" + snapshot.database().latencyMs() + ";desc=\"PostgreSQL\"";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Server-Timing", serverTiming)
                .header("X-Portfolio-Trace", String.join(">",
                        "Spring Security FilterChain",
                        "DispatcherServlet",
                        "EngineeringMissionControlController",
                        "MissionControlService",
                        "DataSource",
                        "PostgreSQL JDBC",
                        "CacheManager",
                        "Caffeine",
                        "BackgroundJobRepository",
                        "OutboxEventRepository",
                        "WebsiteVersionRepository",
                        "Jackson"
                ))
                .body(snapshot);
    }

    @GetMapping("/mission-control/queue")
    public ResponseEntity<EngineeringQueuePageResponse> queue(
            @RequestParam(defaultValue = "analytics") String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long startedAt = System.nanoTime();
        EngineeringQueuePageResponse response = missionControlService.queuePage(kind, page, size);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Server-Timing", "spring;dur=" + elapsedMs(startedAt) + ";desc=\"EngineeringQueue\"")
                .header("X-Portfolio-Trace", queueTrace(kind))
                .body(response);
    }

    @GetMapping("/performance/history")
    public ResponseEntity<PerformanceHistoryResponse> history(
            @RequestParam(defaultValue = "80") int limit
    ) {
        long startedAt = System.nanoTime();
        PerformanceHistoryResponse history = performanceHistoryService.history(limit);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Server-Timing", "spring;dur=" + elapsedMs(startedAt) + ";desc=\"PerformanceHistoryService\"")
                .header("X-Portfolio-Trace", String.join(">",
                        "Spring Security FilterChain",
                        "DispatcherServlet",
                        "EngineeringMissionControlController",
                        "RuntimePerformanceHistoryService",
                        "RuntimePerformanceSampleRepository",
                        "Spring Data JPA",
                        "Hibernate",
                        "PostgreSQL",
                        "Jackson"
                ))
                .body(history);
    }

    @PostMapping("/performance/samples")
    public ResponseEntity<RuntimePerformanceSampleResponse> record(
            @RequestBody @Valid RuntimePerformanceSampleRequest request,
            HttpServletRequest servletRequest
    ) {
        enforceRateLimit(servletRequest);
        long startedAt = System.nanoTime();
        RuntimePerformanceSampleResponse response = performanceHistoryService.record(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .header("Server-Timing", "spring;dur=" + elapsedMs(startedAt) + ";desc=\"PerformanceHistoryService.record\"")
                .header("X-Portfolio-Trace", String.join(">",
                        "Spring Security FilterChain",
                        "DispatcherServlet",
                        "EngineeringMissionControlController",
                        "AnalyticsRateLimiter",
                        "RuntimePerformanceHistoryService",
                        "RuntimePerformanceSampleRepository",
                        "Spring Data JPA",
                        "Hibernate",
                        "PostgreSQL",
                        "Jackson"
                ))
                .body(response);
    }

    private void enforceRateLimit(HttpServletRequest request) {
        if (!rateLimiter.allow(request)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Engineering telemetry rate limit exceeded");
        }
    }

    private String queueTrace(String kind) {
        String normalized = kind == null ? "analytics" : kind.trim().toLowerCase();
        java.util.List<String> components = new java.util.ArrayList<>(java.util.List.of(
                "Spring Security FilterChain",
                "DispatcherServlet",
                "EngineeringMissionControlController",
                "MissionControlService.queuePage"
        ));
        if ("jobs".equals(normalized)) {
            components.addAll(java.util.List.of("BackgroundJobRepository", "Spring Data JPA", "Hibernate", "PostgreSQL"));
        } else if ("outbox".equals(normalized)) {
            components.addAll(java.util.List.of("OutboxEventRepository", "Spring Data JPA", "Hibernate", "PostgreSQL"));
        } else {
            components.addAll(java.util.List.of("AnalyticsIngestionPipeline", "ArrayBlockingQueue"));
        }
        components.add("Jackson");
        return String.join(">", components);
    }

    private double elapsedMs(long startedAt) {
        return Math.round(((System.nanoTime() - startedAt) / 1_000_000.0) * 100.0) / 100.0;
    }

}
