package sorbonne.professional_website.dto.response;

import java.util.List;

public record ProvenSkillResponseDTO(
        String id,
        String label,
        String shortLabel,
        String category,
        String description,
        String summary,
        Integer level,
        Integer score,
        Integer evidenceCount,
        List<String> stacks,
        List<String> proofPoints,
        List<ProjectResponseDTO> projects,
        List<ExperienceResponseDTO> experiences,
        List<String> projectSlugs,
        List<String> experienceTitles
) {
}
