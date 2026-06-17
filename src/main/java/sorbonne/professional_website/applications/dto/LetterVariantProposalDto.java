package sorbonne.professional_website.applications.dto;

import java.util.List;

public record LetterVariantProposalDto(
        String id,
        String templateId,
        String name,
        String angle,
        String tone,
        int score,
        int technicalLevel,
        String plainText,
        String latexSource,
        List<String> usedEvidenceIds,
        List<String> matchedKeywords,
        List<String> strengths,
        List<String> cautions
) {
}
