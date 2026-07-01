package sorbonne.professional_website.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyticsEventRequest(
        @NotBlank @Size(max = 60) String eventType,
        @Size(max = 1000) String pagePath,
        @Size(max = 300) String pageTitle,
        @Size(max = 255) String projectSlug,
        @Size(max = 1200) String referrer,
        @Size(max = 255) String source,
        @Size(max = 255) String medium,
        @Size(max = 255) String campaign,
        @Size(max = 255) String recruiterCode,
        @Size(max = 255) String visitorId,
        @Size(max = 255) String sessionId,
        @Size(max = 50) String deviceType,
        @Size(max = 120) String browser,
        @Size(max = 120) String os,
        @Size(max = 50) String language,
        Integer screenWidth,
        Integer screenHeight
) {
}
