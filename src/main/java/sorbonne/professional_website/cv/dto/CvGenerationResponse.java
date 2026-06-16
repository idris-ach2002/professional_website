package sorbonne.professional_website.cv.dto;

import java.util.List;

public record CvGenerationResponse(
        boolean success,
        String pdfUrl,
        String latexSource,
        String logs,
        List<String> warnings,
        String compiler,
        Long ownerId,
        Long versionId
) {
}
