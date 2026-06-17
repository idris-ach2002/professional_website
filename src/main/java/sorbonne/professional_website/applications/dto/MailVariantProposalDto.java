package sorbonne.professional_website.applications.dto;

import java.util.List;

public record MailVariantProposalDto(
        String subject,
        String body,
        int score,
        List<String> strengths
) {
}
