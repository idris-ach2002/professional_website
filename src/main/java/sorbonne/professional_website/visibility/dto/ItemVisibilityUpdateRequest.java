package sorbonne.professional_website.visibility.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ItemVisibilityUpdateRequest(
        @NotNull Map<String, Boolean> items
) {
}
