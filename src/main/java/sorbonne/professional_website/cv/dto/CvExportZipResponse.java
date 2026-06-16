package sorbonne.professional_website.cv.dto;

import java.util.List;

public record CvExportZipResponse(
        boolean success,
        String zipUrl,
        String pdfUrl,
        String logs,
        List<String> warnings,
        String compiler,
        Long ownerId,
        Long versionId
) {
}
