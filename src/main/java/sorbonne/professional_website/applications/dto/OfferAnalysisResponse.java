package sorbonne.professional_website.applications.dto;

import java.util.List;

public record OfferAnalysisResponse(
        int score,
        String targetProfile,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> recommendedSkills,
        List<String> recommendedProjects,
        List<String> recommendations,
        List<ApplicationCommandDto> commands,
        String summary
) {
}
