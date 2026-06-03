package sorbonne.professional_website.dto;

import java.time.LocalDateTime;

public record ProfileDTO(
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