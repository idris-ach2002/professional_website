package sorbonne.professional_website.engineering.service;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import sorbonne.professional_website.engineering.dto.BackendProfilerSnapshotResponse;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BackendRouteProfiler {

    private static final int SAMPLE_LIMIT = 256;
    private static final Map<String, String> SAFE_BENCHMARK_PATHS = Map.ofEntries(
            Map.entry("GET /website", "/website?locale=fr"),
            Map.entry("GET /website/default", "/website/default?locale=fr"),
            Map.entry("GET /website/default/seo-snapshot", "/website/default/seo-snapshot"),
            Map.entry("GET /api/engineering/mission-control", "/api/engineering/mission-control"),
            Map.entry("GET /api/engineering/mission-control/queue", "/api/engineering/mission-control/queue?kind=analytics&page=0&size=5"),
            Map.entry("GET /api/engineering/performance/history", "/api/engineering/performance/history?limit=20")
    );

    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, RouteStats> observed = new ConcurrentHashMap<>();
    private final AtomicLong activeRequests = new AtomicLong();

    public BackendRouteProfiler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void requestStarted() {
        activeRequests.incrementAndGet();
    }

    public void requestFinished() {
        activeRequests.updateAndGet(value -> Math.max(0, value - 1));
    }

    public void record(
            String method,
            String route,
            String controller,
            String handler,
            int status,
            double wallMs,
            double cpuMs,
            long allocatedBytes
    ) {
        String normalizedMethod = normalizeMethod(method);
        String normalizedRoute = normalizeRoute(route);
        String key = routeKey(normalizedMethod, normalizedRoute);
        observed.computeIfAbsent(key, ignored -> new RouteStats(normalizedMethod, normalizedRoute))
                .record(new RouteSample(
                        Math.max(0, wallMs),
                        cpuMs >= 0 ? cpuMs : -1,
                        allocatedBytes >= 0 ? allocatedBytes : -1,
                        status,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        safeName(controller),
                        safeName(handler)
                ));
    }

    public BackendProfilerSnapshotResponse snapshot() {
        Map<String, CatalogRoute> catalog = routeCatalog();
        Map<String, CatalogRoute> union = new LinkedHashMap<>(catalog);
        observed.forEach((key, stats) -> union.putIfAbsent(key, new CatalogRoute(
                stats.method,
                stats.route,
                stats.controller(),
                stats.handler(),
                SAFE_BENCHMARK_PATHS.get(key)
        )));

        List<BackendProfilerSnapshotResponse.RouteProfile> routes = union.entrySet().stream()
                .map(entry -> toProfile(entry.getKey(), entry.getValue(), observed.get(entry.getKey())))
                .sorted(Comparator
                        .comparing(BackendProfilerSnapshotResponse.RouteProfile::observed).reversed()
                        .thenComparing(BackendProfilerSnapshotResponse.RouteProfile::p95Ms, Comparator.reverseOrder())
                        .thenComparing(BackendProfilerSnapshotResponse.RouteProfile::route)
                        .thenComparing(BackendProfilerSnapshotResponse.RouteProfile::method))
                .toList();

        List<RouteSample> allSamples = observed.values().stream().flatMap(stats -> stats.snapshot().stream()).toList();
        long errors = allSamples.stream().filter(sample -> sample.status >= 500).count();
        double[] durations = allSamples.stream().mapToDouble(RouteSample::wallMs).toArray();

        return new BackendProfilerSnapshotResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                activeRequests.get(),
                systemCost(),
                new BackendProfilerSnapshotResponse.Summary(
                        routes.size(),
                        routes.stream().filter(BackendProfilerSnapshotResponse.RouteProfile::observed).count(),
                        allSamples.size(),
                        errors,
                        average(durations),
                        percentile(durations, .95),
                        percentile(durations, .99)
                ),
                routes
        );
    }

    private Map<String, CatalogRoute> routeCatalog() {
        RequestMappingHandlerMapping mapping;
        try {
            mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        } catch (Exception ignored) {
            return Map.of();
        }
        if (mapping == null) return Map.of();

        Map<String, CatalogRoute> result = new HashMap<>();
        mapping.getHandlerMethods().forEach((info, handlerMethod) -> {
            if (!handlerMethod.getBeanType().getPackageName().startsWith("sorbonne.professional_website")) return;
            Set<String> patterns = info.getPatternValues();
            if (patterns.isEmpty()) return;
            Set<RequestMethod> requestMethods = info.getMethodsCondition().getMethods();
            List<String> methods = requestMethods.isEmpty()
                    ? List.of("ANY")
                    : requestMethods.stream().map(RequestMethod::name).sorted().toList();
            for (String pattern : patterns) {
                for (String method : methods) {
                    String normalizedRoute = normalizeRoute(pattern);
                    if ("/api/engineering/performance/routes".equals(normalizedRoute)) continue;
                    String key = routeKey(method, normalizedRoute);
                    result.put(key, new CatalogRoute(
                            method,
                            normalizedRoute,
                            handlerMethod.getBeanType().getSimpleName(),
                            handlerMethod.getMethod().getName(),
                            SAFE_BENCHMARK_PATHS.get(key)
                    ));
                }
            }
        });
        return result;
    }

    private BackendProfilerSnapshotResponse.RouteProfile toProfile(String key, CatalogRoute catalog, RouteStats stats) {
        List<RouteSample> samples = stats == null ? List.of() : stats.snapshot();
        double[] wall = samples.stream().mapToDouble(RouteSample::wallMs).toArray();
        double[] cpu = samples.stream().mapToDouble(RouteSample::cpuMs).filter(value -> value >= 0).toArray();
        long[] allocated = samples.stream().mapToLong(RouteSample::allocatedBytes).filter(value -> value >= 0).toArray();
        long errors = samples.stream().filter(sample -> sample.status >= 500).count();
        RouteSample last = samples.isEmpty() ? null : samples.getLast();
        String controller = stats != null && !stats.controller().isBlank() ? stats.controller() : catalog.controller;
        String handler = stats != null && !stats.handler().isBlank() ? stats.handler() : catalog.handler;
        return new BackendProfilerSnapshotResponse.RouteProfile(
                catalog.method,
                catalog.route,
                controller,
                handler,
                !samples.isEmpty(),
                catalog.benchmarkPath != null,
                catalog.benchmarkPath,
                samples.size(),
                errors,
                samples.isEmpty() ? 0 : round(errors * 100.0 / samples.size()),
                average(wall),
                percentile(wall, .50),
                percentile(wall, .95),
                percentile(wall, .99),
                max(wall),
                last == null ? -1 : round(last.wallMs),
                average(cpu),
                percentile(cpu, .95),
                average(allocated),
                percentile(allocated, .95),
                last == null ? 0 : last.status,
                last == null ? null : last.at
        );
    }

    private BackendProfilerSnapshotResponse.SystemCost systemCost() {
        java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
        OperatingSystemMXBean operatingSystem = base instanceof OperatingSystemMXBean bean ? bean : null;
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return new BackendProfilerSnapshotResponse.SystemCost(
                percent(operatingSystem == null ? -1 : operatingSystem.getCpuLoad()),
                percent(operatingSystem == null ? -1 : operatingSystem.getProcessCpuLoad()),
                heap.getUsed(),
                heap.getMax()
        );
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) return HttpMethod.GET.name();
        return method.trim().toUpperCase();
    }

    private String normalizeRoute(String route) {
        if (route == null || route.isBlank()) return "/unknown";
        String normalized = route.trim().split("\\?", 2)[0];
        normalized = normalized.replaceAll("/[0-9]+(?=/|$)", "/{id}");
        normalized = normalized.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,36}(?=/|$)", "/{id}");
        return normalized;
    }

    private String routeKey(String method, String route) {
        return normalizeMethod(method) + " " + normalizeRoute(route);
    }

    private String safeName(String value) {
        return value == null ? "" : value.trim();
    }

    private double percent(double ratio) {
        return ratio < 0 ? -1 : round(Math.min(1, ratio) * 100);
    }

    private double average(double[] values) {
        if (values.length == 0) return -1;
        double sum = 0;
        for (double value : values) sum += value;
        return round(sum / values.length);
    }

    private long average(long[] values) {
        if (values.length == 0) return -1;
        long sum = 0;
        for (long value : values) sum += value;
        return Math.round(sum / (double) values.length);
    }

    private double max(double[] values) {
        if (values.length == 0) return -1;
        double max = values[0];
        for (double value : values) max = Math.max(max, value);
        return round(max);
    }

    private double percentile(double[] values, double ratio) {
        if (values.length == 0) return -1;
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        int index = Math.min(sorted.length - 1, Math.max(0, (int) Math.ceil(sorted.length * ratio) - 1));
        return round(sorted[index]);
    }

    private long percentile(long[] values, double ratio) {
        if (values.length == 0) return -1;
        long[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        int index = Math.min(sorted.length - 1, Math.max(0, (int) Math.ceil(sorted.length * ratio) - 1));
        return sorted[index];
    }

    private double round(double value) {
        if (!Double.isFinite(value)) return -1;
        return Math.round(value * 100.0) / 100.0;
    }

    private record RouteSample(double wallMs, double cpuMs, long allocatedBytes, int status, OffsetDateTime at, String controller, String handler) {}
    private record CatalogRoute(String method, String route, String controller, String handler, String benchmarkPath) {}

    private static final class RouteStats {
        private final String method;
        private final String route;
        private final ArrayDeque<RouteSample> samples = new ArrayDeque<>();
        private String controller = "";
        private String handler = "";

        private RouteStats(String method, String route) {
            this.method = method;
            this.route = route;
        }

        private synchronized void record(RouteSample sample) {
            if (!sample.controller.isBlank()) controller = sample.controller;
            if (!sample.handler.isBlank()) handler = sample.handler;
            samples.addLast(sample);
            while (samples.size() > SAMPLE_LIMIT) samples.removeFirst();
        }

        private synchronized List<RouteSample> snapshot() {
            return new ArrayList<>(samples);
        }

        private synchronized String controller() { return controller; }
        private synchronized String handler() { return handler; }
    }
}
