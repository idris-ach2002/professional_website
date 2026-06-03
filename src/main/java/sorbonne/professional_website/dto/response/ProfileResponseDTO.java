package sorbonne.professional_website.dto.response;

import java.time.LocalDateTime;

public record ProfileResponseDTO(
        Long id,
        String title,
        String subtitle,
        String headline,
        String shortDescription,
        String description,
        String location,
        String availability,
        String profileImageUrl,
        String logoUrl,
        String cvUrl,
        String portfolioUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
