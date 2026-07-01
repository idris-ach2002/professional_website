package sorbonne.professional_website.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummaryResponse(
        String from,
        String to,
        long totalEvents,
        long pageViews,
        long uniqueVisitors,
        long sessions,
        long cvClicks,
        long githubClicks,
        long linkedinClicks,
        long projectViews,
        List<DailyMetric> dailyVisits,
        List<MetricItem> topPages,
        List<MetricItem> topProjects,
        List<MetricItem> topSources,
        List<MetricItem> devices,
        List<MetricItem> browsers,
        List<MetricItem> recruiters,
        List<AnalyticsEventResponse> recentEvents
) {
    public record DailyMetric(LocalDate date, long pageViews, long uniqueVisitors) {
    }

    public record MetricItem(String label, long value) {
    }
}
