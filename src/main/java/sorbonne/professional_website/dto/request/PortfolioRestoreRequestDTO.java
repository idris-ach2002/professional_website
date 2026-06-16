package sorbonne.professional_website.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record PortfolioRestoreRequestDTO(
        @Size(max = 2_000_000, message = "Le JSON de backup est trop volumineux.")
        String backupJson,

        @Valid
        WebsiteVersionRequestDTO version,

        @Size(max = 160, message = "Le libellé restauré ne doit pas dépasser 160 caractères.")
        String restoreLabel,

        Boolean active
) {
}
