package sorbonne.professional_website.applications.dto;

public record SmartApplicationPackResponse(
        boolean success,
        String zipUrl,
        String message,
        Long ownerId,
        Long applicationId
) {
}
