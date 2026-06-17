package sorbonne.professional_website.applications.dto;

import java.util.List;

public record StructuredOfferDto(
        String companyName,
        String sector,
        String tone,
        String roleTitle,
        String contractType,
        String location,
        String seniority,
        List<String> hardSkills,
        List<String> softSkills,
        List<String> missions,
        List<String> mustHave,
        List<String> niceToHave,
        List<String> implicitExpectations,
        List<String> atsKeywords
) {
}
