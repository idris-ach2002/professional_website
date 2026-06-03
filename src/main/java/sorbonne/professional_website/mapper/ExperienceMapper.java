package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.ExperienceDTO;
import sorbonne.professional_website.entity.Experience;

import java.util.ArrayList;
import java.util.List;

public final class ExperienceMapper {

    private ExperienceMapper() {
    }

    public static ExperienceDTO toDTO(Experience experience) {
        if (experience == null) {
            return null;
        }

        return new ExperienceDTO(
                experience.getCategory(),
                experience.getTitle(),
                experience.getOrganization(),
                experience.getLocation(),
                experience.getSummary(),
                experience.getDescription(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.isCurrentPosition(),
                experience.getImageUrl(),
                experience.getWebsiteUrl(),
                experience.getSkills(),
                experience.getDisplayOrder()
        );
    }

    public static List<ExperienceDTO> toDTOList(List<Experience> experiences) {
        if (experiences == null) {
            return List.of();
        }

        List<ExperienceDTO> experienceDTOs = new ArrayList<>();

        for (Experience experience : experiences) {
            experienceDTOs.add(toDTO(experience));
        }

        return experienceDTOs;
    }
}