package sorbonne.professional_website.applications.dto;

public record MatchingScoreDto(
        int globalScore,
        int hardSkillsScore,
        int softSkillsScore,
        int missionScore,
        int evidenceScore,
        int atsScore,
        int riskScore
) {
}
