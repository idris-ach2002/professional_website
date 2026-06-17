package sorbonne.professional_website.applications.dto;

public record ApplicationCommandDto(
        String type,
        String target,
        String value
) {
}
