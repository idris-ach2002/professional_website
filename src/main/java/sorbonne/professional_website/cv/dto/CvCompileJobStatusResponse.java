package sorbonne.professional_website.cv.dto;

import java.util.List;

public record CvCompileJobStatusResponse(
        String jobId,
        String status,
        int progress,
        String step,
        String pdfUrl,
        String latexSource,
        String logs,
        List<String> warnings,
        String compiler,
        Long ownerId,
        Long versionId
) {
}
