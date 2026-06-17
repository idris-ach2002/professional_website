package sorbonne.professional_website.applications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import sorbonne.professional_website.cv.dto.CvAssetDto;

import java.util.List;

public record CoverLetterRequest(
        Long versionId,

        @Size(max = 120_000)
        String latexSourceOverride,

        @Size(max = 20_000)
        String motivationTextOverride,

        @Size(max = 160)
        String templateId,

        @Valid
        List<CvAssetDto> assets
) {
}
