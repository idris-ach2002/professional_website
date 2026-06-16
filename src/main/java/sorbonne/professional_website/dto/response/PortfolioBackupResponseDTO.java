package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;

public record PortfolioBackupResponseDTO(
        boolean success,
        String filename,
        String url,
        String json,
        long bytes,
        LocalDateTime generatedAt,
        Long ownerId,
        Long versionId
) {
}
