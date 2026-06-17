package sorbonne.professional_website.applications.dto;

import jakarta.validation.constraints.Size;
import sorbonne.professional_website.applications.entity.ApplicationStatus;

import java.time.LocalDate;

public record JobApplicationRequest(
        Long versionId,

        @Size(max = 180)
        String companyName,

        @Size(max = 220)
        String roleTitle,

        @Size(max = 180)
        String location,

        @Size(max = 1000)
        String offerUrl,

        @Size(max = 80_000)
        String offerText,

        ApplicationStatus status,

        @Size(max = 160)
        String targetProfile,

        @Size(max = 160)
        String cvVariantName,

        @Size(max = 1000)
        String cvUrl,

        @Size(max = 1000)
        String coverLetterUrl,

        @Size(max = 12_000)
        String mailDraft,

        @Size(max = 120_000)
        String coverLetterSource,

        @Size(max = 20_000)
        String notes,

        LocalDate appliedAt,
        LocalDate followUpAt
) {
}
