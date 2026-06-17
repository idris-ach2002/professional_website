package sorbonne.professional_website.applications.dto;

import java.util.List;

public record LetterTemplateResponse(
        String id,
        String name,
        String category,
        String tone,
        int technicalLevel,
        List<String> bestFor,
        String angle,
        String structure,
        boolean builtIn
) {
}
