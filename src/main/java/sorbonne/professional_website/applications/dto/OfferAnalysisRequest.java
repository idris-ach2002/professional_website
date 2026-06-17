package sorbonne.professional_website.applications.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record OfferAnalysisRequest(
        @Size(max = 80_000)
        String offerText,

        @Size(max = 220)
        String roleTitle,

        @Size(max = 180)
        String companyName,

        List<String> portfolioSkills,
        List<String> projectTitles
) {
}
