package sorbonne.professional_website.dto.request;

public record ProfileRequestDTO(
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
        String portfolioUrl
) {
}
