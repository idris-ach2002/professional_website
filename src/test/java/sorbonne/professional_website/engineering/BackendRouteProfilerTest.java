package sorbonne.professional_website.engineering;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import sorbonne.professional_website.engineering.service.BackendRouteProfiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BackendRouteProfilerTest {

    @Test
    void recordsWallCpuAllocationAndErrorsWithoutExposingRawIds() {
        BackendRouteProfiler profiler = new BackendRouteProfiler(mock(ApplicationContext.class));
        profiler.record("GET", "/website/42", "WebsiteController", "getWebsiteByOwner", 200, 18.4, 7.1, 8192);
        profiler.record("GET", "/website/42", "WebsiteController", "getWebsiteByOwner", 500, 28.2, 9.3, 12288);

        var snapshot = profiler.snapshot();
        var route = snapshot.routes().stream().filter(item -> "/website/{id}".equals(item.route())).findFirst().orElseThrow();

        assertThat(route.sampleCount()).isEqualTo(2);
        assertThat(route.errorCount()).isEqualTo(1);
        assertThat(route.p95Ms()).isEqualTo(28.2);
        assertThat(route.averageCpuMs()).isEqualTo(8.2);
        assertThat(route.averageAllocatedBytes()).isEqualTo(10240);
        assertThat(route.route()).doesNotContain("42");
    }
}
