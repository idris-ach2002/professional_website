package sorbonne.professional_website.applications.dto;

import java.util.List;

public record CoverLetterResponse(
        boolean success,
        String pdfUrl,
        String zipUrl,
        String latexSource,
        String logs,
        List<String> warnings,
        String compiler,
        Long applicationId,
        Long ownerId
) {
}
