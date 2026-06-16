package sorbonne.professional_website.cv.dto;

public record CvCompileJobResponse(
        String jobId,
        String status,
        String message,
        Long ownerId,
        Long versionId
) {
}
