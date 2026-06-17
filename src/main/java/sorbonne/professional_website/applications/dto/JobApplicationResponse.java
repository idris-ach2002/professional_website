package sorbonne.professional_website.applications.dto;

import sorbonne.professional_website.applications.entity.ApplicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JobApplicationResponse(
        Long id,
        Long ownerId,
        Long versionId,
        String companyName,
        String roleTitle,
        String location,
        String offerUrl,
        String offerText,
        ApplicationStatus status,
        String targetProfile,
        String cvVariantName,
        String cvUrl,
        String coverLetterUrl,
        String applicationZipUrl,
        String mailDraft,
        String coverLetterSource,
        String notes,
        Integer relevanceScore,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> recommendations,
        LocalDate appliedAt,
        LocalDate followUpAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
