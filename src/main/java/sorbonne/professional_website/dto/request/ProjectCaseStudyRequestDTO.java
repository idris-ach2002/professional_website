package sorbonne.professional_website.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectCaseStudyRequestDTO(
        @Size(max = 5000, message = "Le problème du case study ne doit pas dépasser 5000 caractères.")
        String problem,

        @Size(max = 5000, message = "Le contexte du case study ne doit pas dépasser 5000 caractères.")
        String context,

        @Size(max = 5000, message = "Le rôle du case study ne doit pas dépasser 5000 caractères.")
        String role,

        @Size(max = 5000, message = "L'architecture du case study ne doit pas dépasser 5000 caractères.")
        String architecture,

        List<@Size(max = 1000, message = "Un choix technique ne doit pas dépasser 1000 caractères.") String> technicalChoices,
        List<@Size(max = 1000, message = "Une difficulté ne doit pas dépasser 1000 caractères.") String> challenges,
        List<@Size(max = 1000, message = "Une solution ne doit pas dépasser 1000 caractères.") String> solutions,
        List<@Size(max = 1000, message = "Un impact ne doit pas dépasser 1000 caractères.") String> outcomes,
        List<@Size(max = 1000, message = "Un résultat ne doit pas dépasser 1000 caractères.") String> results,
        List<@Size(max = 1000, message = "Une limite ne doit pas dépasser 1000 caractères.") String> limits,

        @Size(max = 5000, message = "Les prochaines étapes ne doivent pas dépasser 5000 caractères.")
        String nextSteps
) {
}
