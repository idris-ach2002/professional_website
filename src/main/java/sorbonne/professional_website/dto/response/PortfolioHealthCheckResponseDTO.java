package sorbonne.professional_website.dto.response;

public record PortfolioHealthCheckResponseDTO(
        String id,
        String label,
        String severity,
        String status,
        String message
) {
}
