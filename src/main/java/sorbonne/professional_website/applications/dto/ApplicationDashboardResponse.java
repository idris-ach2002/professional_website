package sorbonne.professional_website.applications.dto;

import java.util.Map;

public record ApplicationDashboardResponse(
        long total,
        long toPrepare,
        long sent,
        long followUp,
        long interview,
        long accepted,
        long rejected,
        double averageScore,
        Map<String, Long> byStatus
) {
}
