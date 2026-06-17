package sorbonne.professional_website.applications.dto;

import java.util.List;

public record CandidateEvidenceDto(
        String id,
        String type,
        String title,
        String subtitle,
        int score,
        List<String> matchedKeywords,
        List<String> evidenceTags,
        String proofText,
        String cvBullet,
        String letterSentence,
        boolean recommended
) {
}
