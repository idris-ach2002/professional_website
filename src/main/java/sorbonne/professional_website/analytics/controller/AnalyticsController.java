package sorbonne.professional_website.analytics.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.analytics.dto.AnalyticsEventRequest;
import sorbonne.professional_website.analytics.dto.AnalyticsEventResponse;
import sorbonne.professional_website.analytics.dto.AnalyticsSummaryResponse;

import java.util.List;
import sorbonne.professional_website.analytics.service.AnalyticsService;

import java.time.LocalDate;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/analytics/events")
    public ResponseEntity<AnalyticsEventResponse> trackEvent(
            @RequestBody @Valid AnalyticsEventRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analyticsService.track(request, servletRequest));
    }

    @GetMapping("/manager/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "80") int recentLimit
    ) {
        return ResponseEntity.ok(analyticsService.summary(from, to, recentLimit));
    }
    @GetMapping("/manager/analytics/events")
    public ResponseEntity<List<AnalyticsEventResponse>> getRecentEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(analyticsService.recentEvents(from, to, limit));
    }

}
