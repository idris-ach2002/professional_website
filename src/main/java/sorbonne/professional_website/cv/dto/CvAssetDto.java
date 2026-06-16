package sorbonne.professional_website.cv.dto;

import jakarta.validation.constraints.Size;

public record CvAssetDto(
        @Size(max = 96)
        String filename,

        @Size(max = 50)
        String mimeType,

        @Size(max = 8_000_000)
        String dataUrl
) {
}
