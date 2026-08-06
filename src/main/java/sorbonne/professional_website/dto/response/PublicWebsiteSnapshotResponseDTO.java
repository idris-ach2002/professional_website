package sorbonne.professional_website.dto.response;

public record PublicWebsiteSnapshotResponseDTO(
        String generatedAt,
        OwnerResponseDTO fr,
        OwnerResponseDTO en
) {
}
