package sorbonne.professional_website.cv.dto;

public record CvSourceResponse(
        String latexSource,
        String templateId,
        Long ownerId,
        Long versionId
) {
}
