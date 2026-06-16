package sorbonne.professional_website.cv.dto;

import java.util.List;

public record CvQualityReportResponse(
        int score,
        int estimatedPages,
        List<String> blockers,
        List<String> warnings,
        List<String> suggestions,
        Long ownerId,
        Long versionId
) {
}
