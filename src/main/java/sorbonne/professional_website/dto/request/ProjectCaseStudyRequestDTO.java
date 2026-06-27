package sorbonne.professional_website.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectCaseStudyRequestDTO(
        String problem,
        String context,
        String role,
        String architecture,

        List<@Size(max = 500, message = "Un choix technique ne doit pas dépasser 500 caractères.") String> technicalChoices,
        List<@Size(max = 500, message = "Une difficulté ne doit pas dépasser 500 caractères.") String> challenges,
        List<@Size(max = 500, message = "Une solution ne doit pas dépasser 500 caractères.") String> solutions,
        List<@Size(max = 500, message = "Un résultat ne doit pas dépasser 500 caractères.") String> outcomes,
        List<@Size(max = 500, message = "Un résultat ne doit pas dépasser 500 caractères.") String> results,
        List<@Size(max = 500, message = "Une limite ne doit pas dépasser 500 caractères.") String> limits,

        String nextSteps
) {
}
