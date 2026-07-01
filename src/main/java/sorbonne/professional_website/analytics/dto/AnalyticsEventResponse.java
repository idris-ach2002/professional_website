package sorbonne.professional_website.analytics.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnalyticsEventResponse(
        UUID id,
        String eventType,
        String pagePath,
        String pageTitle,
        String projectSlug,
        String referrer,
        String source,
        String medium,
        String campaign,
        String recruiterCode,
        String visitorIdHash,
        String sessionIdHash,
        String deviceType,
        String browser,
        String os,
        String language,
        Integer screenWidth,
        Integer screenHeight,
        String country,
        OffsetDateTime createdAt
) {
}
