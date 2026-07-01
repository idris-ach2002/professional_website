package sorbonne.professional_website.analytics.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    private final AnalyticsEventRepository analyticsEventRepository;
    private final String hashSecret;

    public AnalyticsService(
            AnalyticsEventRepository analyticsEventRepository,
            @Value("${app.analytics.hash-secret}") String hashSecret
    ) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.hashSecret = hashSecret;
    }

    @Transactional
    public AnalyticsEventResponse track(AnalyticsEventRequest request, HttpServletRequest servletRequest) {
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
                .build();

        return toResponse(analyticsEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(LocalDate from, LocalDate to, int recentLimit) {
        LocalDate safeTo = to == null ? LocalDate.now(PARIS_ZONE) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(30) : from;

        if (safeFrom.isAfter(safeTo)) {
            LocalDate previousFrom = safeFrom;
            safeFrom = safeTo;
            safeTo = previousFrom;
        }

        OffsetDateTime fromDateTime = safeFrom.atStartOfDay(PARIS_ZONE).toOffsetDateTime();
        OffsetDateTime toDateTime = safeTo.atTime(LocalTime.MAX).atZone(PARIS_ZONE).toOffsetDateTime();

        List<AnalyticsEvent> events = analyticsEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                fromDateTime,
                toDateTime
        );

        List<AnalyticsEventResponse> recentEvents = analyticsEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        fromDateTime,
                        toDateTime,
                        PageRequest.of(0, Math.max(1, Math.min(recentLimit, 200)))
                )
                .stream()
                .map(this::toResponse)
                .toList();

        long pageViews = countType(events, "page_view");
        long cvClicks = countType(events, "cv_click");
        long githubClicks = countType(events, "github_click");
        long linkedinClicks = countType(events, "linkedin_click");
        long projectViews = countType(events, "project_view");

        long uniqueVisitors = events.stream()
                .map(AnalyticsEvent::getVisitorIdHash)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long sessions = events.stream()
                .map(AnalyticsEvent::getSessionIdHash)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new AnalyticsSummaryResponse(
                safeFrom.toString(),
                safeTo.toString(),
                events.size(),
                pageViews,
                uniqueVisitors,
                sessions,
                cvClicks,
                githubClicks,
                linkedinClicks,
                projectViews,
                dailyMetrics(events, safeFrom, safeTo),
                topMetrics(events, AnalyticsEvent::getPagePath, 8),
                topMetrics(events, AnalyticsEvent::getProjectSlug, 8),
                topMetrics(events, AnalyticsEvent::getSource, 8),
                topMetrics(events, AnalyticsEvent::getDeviceType, 8),
                topMetrics(events, AnalyticsEvent::getBrowser, 8),
                topMetrics(events, AnalyticsEvent::getRecruiterCode, 8),
                recentEvents
        );
    }


    @Transactional(readOnly = true)
    public List<AnalyticsEventResponse> recentEvents(LocalDate from, LocalDate to, int limit) {
        LocalDate safeTo = to == null ? LocalDate.now(PARIS_ZONE) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(30) : from;

        if (safeFrom.isAfter(safeTo)) {
            LocalDate previousFrom = safeFrom;
            safeFrom = safeTo;
            safeTo = previousFrom;
        }

        OffsetDateTime fromDateTime = safeFrom.atStartOfDay(PARIS_ZONE).toOffsetDateTime();
        OffsetDateTime toDateTime = safeTo.atTime(LocalTime.MAX).atZone(PARIS_ZONE).toOffsetDateTime();

        return analyticsEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        fromDateTime,
                        toDateTime,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 300)))
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private List<AnalyticsSummaryResponse.DailyMetric> dailyMetrics(
            List<AnalyticsEvent> events,
            LocalDate from,
            LocalDate to
    ) {
        Map<LocalDate, List<AnalyticsEvent>> byDate = events.stream()
                .collect(Collectors.groupingBy(event -> event.getCreatedAt().atZoneSameInstant(PARIS_ZONE).toLocalDate()));

        return from.datesUntil(to.plusDays(1))
                .map(date -> {
                    List<AnalyticsEvent> dayEvents = byDate.getOrDefault(date, List.of());
                    long dayPageViews = countType(dayEvents, "page_view");
                    long dayUniqueVisitors = dayEvents.stream()
                            .map(AnalyticsEvent::getVisitorIdHash)
                            .filter(Objects::nonNull)
                            .distinct()
                            .count();
                    return new AnalyticsSummaryResponse.DailyMetric(date, dayPageViews, dayUniqueVisitors);
                })
                .toList();
    }

    private List<AnalyticsSummaryResponse.MetricItem> topMetrics(
            List<AnalyticsEvent> events,
            java.util.function.Function<AnalyticsEvent, String> extractor,
            int limit
    ) {
        return events.stream()
                .map(extractor)
                .map(value -> value == null || value.isBlank() ? "Non renseigné" : value.trim())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> new AnalyticsSummaryResponse.MetricItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private long countType(List<AnalyticsEvent> events, String type) {
        return events.stream()
                .filter(event -> type.equalsIgnoreCase(event.getEventType()))
                .count();
    }

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
