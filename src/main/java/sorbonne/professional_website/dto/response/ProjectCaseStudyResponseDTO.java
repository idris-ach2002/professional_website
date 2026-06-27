package sorbonne.professional_website.dto.response;

import java.util.List;

public record ProjectCaseStudyResponseDTO(
        String problem,
        String context,
        String role,
        String architecture,
        List<String> technicalChoices,
        List<String> challenges,
        List<String> solutions,
        List<String> outcomes,
        List<String> results,
        List<String> limits,
        String nextSteps
) {
}
