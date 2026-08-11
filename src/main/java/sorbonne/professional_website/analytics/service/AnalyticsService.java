package sorbonne.professional_website.analytics.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.analytics.dto.AnalyticsEventRequest;
import sorbonne.professional_website.analytics.dto.AnalyticsEventResponse;
import sorbonne.professional_website.analytics.dto.AnalyticsSummaryResponse;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;
import sorbonne.professional_website.analytics.repository.AnalyticsEventRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    private final AnalyticsEventRepository analyticsEventRepository;
    private final String hashSecret;
    private final AnalyticsIngestionPipeline ingestionPipeline;
    private final AnalyticsRateLimiter rateLimiter;

    public AnalyticsService(
            AnalyticsEventRepository analyticsEventRepository,
            @Value("${app.analytics.hash-secret}") String hashSecret,
            AnalyticsIngestionPipeline ingestionPipeline,
            AnalyticsRateLimiter rateLimiter
    ) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.hashSecret = hashSecret;
        this.ingestionPipeline = ingestionPipeline;
        this.rateLimiter = rateLimiter;
    }

    public AnalyticsEventResponse track(AnalyticsEventRequest request, HttpServletRequest servletRequest) {
        if (!rateLimiter.allow(servletRequest)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Analytics rate limit exceeded");
        }
        String userAgent = trim(servletRequest.getHeader("User-Agent"), 600);
        String country = firstNonBlank(
                servletRequest.getHeader("CF-IPCountry"),
                servletRequest.getHeader("X-Vercel-IP-Country")
        );

        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(normalizeEventType(request.eventType()))
                .pagePath(trim(request.pagePath(), 1000))
                .pageTitle(trim(request.pageTitle(), 300))
                .projectSlug(trim(request.projectSlug(), 255))
                .referrer(trim(request.referrer(), 1200))
                .source(trim(request.source(), 255))
                .medium(trim(request.medium(), 255))
                .campaign(trim(request.campaign(), 255))
                .recruiterCode(trim(request.recruiterCode(), 255))
                .visitorIdHash(hashNullable(request.visitorId()))
                .sessionIdHash(hashNullable(request.sessionId()))
                .deviceType(trim(request.deviceType(), 50))
                .browser(trim(firstNonBlank(request.browser(), detectBrowser(userAgent)), 120))
                .os(trim(firstNonBlank(request.os(), detectOs(userAgent)), 120))
                .language(trim(request.language(), 50))
                .screenWidth(safePositiveInteger(request.screenWidth()))
                .screenHeight(safePositiveInteger(request.screenHeight()))
                .userAgent(userAgent)
                .country(trim(country, 100))
                .createdAt(OffsetDateTime.now())
                .build();

        if (!ingestionPipeline.offer(event)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Analytics queue is saturated");
        }
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(LocalDate from, LocalDate to, int recentLimit) {
        DateRange range = normalizeRange(from, to);
        AnalyticsEventRepository.AnalyticsTotalsView totals = analyticsEventRepository.aggregateTotals(
                range.fromDateTime(),
                range.toDateTime()
        );

        int topLimit = 8;
        List<AnalyticsEventResponse> recentEvents = analyticsEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        range.fromDateTime(),
                        range.toDateTime(),
                        PageRequest.of(0, Math.max(1, Math.min(recentLimit, 200)))
                )
                .stream()
                .map(this::toResponse)
                .toList();

        Map<LocalDate, AnalyticsEventRepository.DailyMetricView> dailyByDate = analyticsEventRepository
                .aggregateDaily(range.fromDateTime(), range.toDateTime())
                .stream()
                .collect(java.util.stream.Collectors.toMap(AnalyticsEventRepository.DailyMetricView::getDay, item -> item));

        List<AnalyticsSummaryResponse.DailyMetric> daily = range.from().datesUntil(range.to().plusDays(1))
                .map(date -> {
                    AnalyticsEventRepository.DailyMetricView metric = dailyByDate.get(date);
                    return new AnalyticsSummaryResponse.DailyMetric(
                            date,
                            metric == null ? 0L : metric.getPageViews(),
                            metric == null ? 0L : metric.getUniqueVisitors()
                    );
                })
                .toList();

        return new AnalyticsSummaryResponse(
                range.from().toString(),
                range.to().toString(),
                totals == null ? 0L : totals.getTotalEvents(),
                totals == null ? 0L : totals.getPageViews(),
                totals == null ? 0L : totals.getUniqueVisitors(),
                totals == null ? 0L : totals.getSessions(),
                totals == null ? 0L : totals.getCvClicks(),
                totals == null ? 0L : totals.getGithubClicks(),
                totals == null ? 0L : totals.getLinkedinClicks(),
                totals == null ? 0L : totals.getProjectViews(),
                daily,
                metricItems(analyticsEventRepository.topPages(range.fromDateTime(), range.toDateTime(), topLimit)),
                metricItems(analyticsEventRepository.topProjects(range.fromDateTime(), range.toDateTime(), topLimit)),
                metricItems(analyticsEventRepository.topSources(range.fromDateTime(), range.toDateTime(), topLimit)),
                metricItems(analyticsEventRepository.topDevices(range.fromDateTime(), range.toDateTime(), topLimit)),
                metricItems(analyticsEventRepository.topBrowsers(range.fromDateTime(), range.toDateTime(), topLimit)),
                metricItems(analyticsEventRepository.topRecruiters(range.fromDateTime(), range.toDateTime(), topLimit)),
                recentEvents
        );
    }


    @Transactional(readOnly = true)
    public List<AnalyticsEventResponse> recentEvents(LocalDate from, LocalDate to, int limit) {
        DateRange range = normalizeRange(from, to);

        return analyticsEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        range.fromDateTime(),
                        range.toDateTime(),
                        PageRequest.of(0, Math.max(1, Math.min(limit, 300)))
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private List<AnalyticsSummaryResponse.MetricItem> metricItems(
            List<AnalyticsEventRepository.MetricCountView> metrics
    ) {
        return metrics.stream()
                .map(item -> new AnalyticsSummaryResponse.MetricItem(item.getLabel(), item.getValue()))
                .toList();
    }

    private DateRange normalizeRange(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now(PARIS_ZONE) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(30) : from;
        if (safeFrom.isAfter(safeTo)) {
            LocalDate previousFrom = safeFrom;
            safeFrom = safeTo;
            safeTo = previousFrom;
        }
        return new DateRange(
                safeFrom,
                safeTo,
                safeFrom.atStartOfDay(PARIS_ZONE).toOffsetDateTime(),
                safeTo.atTime(LocalTime.MAX).atZone(PARIS_ZONE).toOffsetDateTime()
        );
    }

    private record DateRange(
            LocalDate from,
            LocalDate to,
            OffsetDateTime fromDateTime,
            OffsetDateTime toDateTime
    ) { }

    private AnalyticsEventResponse toResponse(AnalyticsEvent event) {
        return new AnalyticsEventResponse(
                event.getId(),
                event.getEventType(),
                event.getPagePath(),
                event.getPageTitle(),
                event.getProjectSlug(),
                event.getReferrer(),
                event.getSource(),
                event.getMedium(),
                event.getCampaign(),
                event.getRecruiterCode(),
                event.getVisitorIdHash(),
                event.getSessionIdHash(),
                event.getDeviceType(),
                event.getBrowser(),
                event.getOs(),
                event.getLanguage(),
                event.getScreenWidth(),
                event.getScreenHeight(),
                event.getCountry(),
                event.getCreatedAt()
        );
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "event";
        }
        return eventType.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private Integer safePositiveInteger(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return Math.min(value, 10000);
    }

    private String hashNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((hashSecret + ":" + value.trim()).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) return null;
        String lower = userAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("firefox")) return "Firefox";
        if (lower.contains("edg/")) return "Edge";
        if (lower.contains("opr/") || lower.contains("opera")) return "Opera";
        if (lower.contains("chrome") || lower.contains("chromium")) return "Chrome";
        if (lower.contains("safari")) return "Safari";
        return "Autre";
    }

    private String detectOs(String userAgent) {
        if (userAgent == null) return null;
        String lower = userAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("windows")) return "Windows";
        if (lower.contains("android")) return "Android";
        if (lower.contains("iphone") || lower.contains("ipad")) return "iOS";
        if (lower.contains("mac os") || lower.contains("macintosh")) return "macOS";
        if (lower.contains("linux")) return "Linux";
        return "Autre";
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String trim(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isBlank()) return null;
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
