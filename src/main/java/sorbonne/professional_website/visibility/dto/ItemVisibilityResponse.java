package sorbonne.professional_website.visibility.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ItemVisibilityResponse(
        Map<String, Boolean> items,
        OffsetDateTime updatedAt
) {
}
