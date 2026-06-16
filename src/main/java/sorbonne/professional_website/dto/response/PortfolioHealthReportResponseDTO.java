package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PortfolioHealthReportResponseDTO(
        int score,
        boolean publishable,
        int blockersCount,
        int warningsCount,
        int suggestionsCount,
        List<PortfolioHealthCheckResponseDTO> checks,
        LocalDateTime generatedAt,
        Long ownerId,
        Long versionId
) {
}
