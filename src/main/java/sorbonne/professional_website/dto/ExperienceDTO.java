package sorbonne.professional_website.dto;

import sorbonne.professional_website.entity.enumerations.CategoryExperience;

import java.time.LocalDate;
import java.util.List;

public record ExperienceDTO(
        CategoryExperience category,
        String title,
        String organization,
        String location,
        String summary,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentPosition,
        String imageUrl,
        String websiteUrl,
        List<String> skills,
        Integer displayOrder
) {
}