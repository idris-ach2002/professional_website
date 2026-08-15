package sorbonne.professional_website.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import sorbonne.professional_website.engineering.service.BackendRouteProfiler;

import java.io.IOException;
import java.lang.management.ManagementFactory;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class BackendRouteProfilerFilter extends OncePerRequestFilter {

    private final BackendRouteProfiler profiler;
    private final java.lang.management.ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    private final com.sun.management.ThreadMXBean allocationMxBean;

    public BackendRouteProfilerFilter(BackendRouteProfiler profiler) {
        this.profiler = profiler;
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        this.allocationMxBean = bean instanceof com.sun.management.ThreadMXBean sunBean ? sunBean : null;
        enableThreadMeasurements();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return true;
        if (path.equals("/api/engineering/performance/routes")) return true;
        return !(path.equals("/")
                || path.startsWith("/api/")
                || path.startsWith("/website")
                || path.startsWith("/uploads")
                || path.startsWith("/manager")
                || path.startsWith("/analytics")
                || path.startsWith("/publication")
                || path.startsWith("/admin")
                || path.equals("/csrf")
                || path.equals("/login")
                || path.equals("/logout"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedNs = System.nanoTime();
        long cpuStartedNs = currentThreadCpuTime();
        long allocatedStartedBytes = currentThreadAllocatedBytes();
        profiler.requestStarted();
        try {
            filterChain.doFilter(request, response);
        } finally {
            double wallMs = (System.nanoTime() - startedNs) / 1_000_000.0;
            long cpuEndedNs = currentThreadCpuTime();
            long allocatedEndedBytes = currentThreadAllocatedBytes();
            double cpuMs = cpuStartedNs >= 0 && cpuEndedNs >= cpuStartedNs
                    ? (cpuEndedNs - cpuStartedNs) / 1_000_000.0
                    : -1;
            long allocatedBytes = allocatedStartedBytes >= 0 && allocatedEndedBytes >= allocatedStartedBytes
                    ? allocatedEndedBytes - allocatedStartedBytes
                    : -1;
            Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            Object bestHandler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
            // Never expose an unmatched raw URI: it can contain a filename, slug or identifier.
            // Known handlers provide their templated Spring pattern after the chain.
            String route = bestPattern == null ? "/unmatched" : String.valueOf(bestPattern);
            String controller = "";
            String handler = "";
            if (bestHandler instanceof HandlerMethod handlerMethod) {
                controller = handlerMethod.getBeanType().getSimpleName();
                handler = handlerMethod.getMethod().getName();
            }
            try {
                profiler.record(request.getMethod(), route, controller, handler, response.getStatus(), wallMs, cpuMs, allocatedBytes);
            } finally {
                profiler.requestFinished();
            }
        }
    }

    private void enableThreadMeasurements() {
        try {
            if (threadMxBean.isThreadCpuTimeSupported() && !threadMxBean.isThreadCpuTimeEnabled()) {
                threadMxBean.setThreadCpuTimeEnabled(true);
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // CPU time remains explicitly unavailable.
        }
        try {
            if (allocationMxBean != null
                    && allocationMxBean.isThreadAllocatedMemorySupported()
                    && !allocationMxBean.isThreadAllocatedMemoryEnabled()) {
                allocationMxBean.setThreadAllocatedMemoryEnabled(true);
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Allocation tracking remains explicitly unavailable.
        }
    }

    private long currentThreadCpuTime() {
        try {
            if (!threadMxBean.isCurrentThreadCpuTimeSupported() || !threadMxBean.isThreadCpuTimeEnabled()) return -1;
            return threadMxBean.getCurrentThreadCpuTime();
        } catch (UnsupportedOperationException | SecurityException ignored) {
            return -1;
        }
    }

    private long currentThreadAllocatedBytes() {
        try {
            if (allocationMxBean == null
                    || !allocationMxBean.isThreadAllocatedMemorySupported()
                    || !allocationMxBean.isThreadAllocatedMemoryEnabled()) return -1;
            return allocationMxBean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        } catch (UnsupportedOperationException | SecurityException ignored) {
            return -1;
        }
    }
}
