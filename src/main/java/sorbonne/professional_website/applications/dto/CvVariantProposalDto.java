package sorbonne.professional_website.applications.dto;

import java.util.List;

public record CvVariantProposalDto(
        String id,
        String name,
        String strategy,
        int score,
        String targetTitle,
        String headline,
        List<String> prioritizedKeywords,
        List<String> prioritizedEvidenceIds,
        List<String> hiddenOrReducedElements,
        List<ApplicationCommandDto> commands,
        List<String> explanations
) {
}
