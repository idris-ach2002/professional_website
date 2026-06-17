package sorbonne.professional_website.applications.dto;

import java.util.List;

public record SmartOfferAnalysisResponse(
        String analysisId,
        Long ownerId,
        Long applicationId,
        Long versionId,
        StructuredOfferDto offer,
        MatchingScoreDto scores,
        List<String> matchedKeywords,
        List<String> missingCriticalKeywords,
        List<String> recommendations,
        List<String> riskWarnings,
        List<CandidateEvidenceDto> evidence,
        List<CvVariantProposalDto> cvVariants,
        List<LetterVariantProposalDto> letterVariants,
        MailVariantProposalDto mail,
        List<ApplicationCommandDto> recommendedCommands,
        String explanation
) {
}
