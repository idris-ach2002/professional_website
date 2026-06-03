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
                experience.getId(),
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

    public static Experience fromCreateDTO(ExperienceDTO experienceDTO) {
        if (experienceDTO == null) {
            return null;
        }

        Experience experience = new Experience();

        setPropertyExperience(experienceDTO, experience);

        return experience;
    }

    private static void setPropertyExperience(ExperienceDTO experienceDTO, Experience experience) {
        experience.setCategory(experienceDTO.category());
        experience.setTitle(experienceDTO.title());
        experience.setOrganization(experienceDTO.organization());
        experience.setLocation(experienceDTO.location());
        experience.setSummary(experienceDTO.summary());
        experience.setDescription(experienceDTO.description());
        experience.setStartDate(experienceDTO.startDate());
        experience.setEndDate(experienceDTO.endDate());
        experience.setCurrentPosition(experienceDTO.currentPosition());
        experience.setImageUrl(experienceDTO.imageUrl());
        experience.setWebsiteUrl(experienceDTO.websiteUrl());
        experience.setSkills(toStringList(experienceDTO.skills()));
        experience.setDisplayOrder(experienceDTO.displayOrder());
    }

    public static void updateEntityFromDTO(Experience experience, ExperienceDTO experienceDTO) {
        if (experience == null || experienceDTO == null) {
            return;
        }

        setPropertyExperience(experienceDTO, experience);
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

    public static List<Experience> fromDTOList(List<ExperienceDTO> experienceDTOs) {
        if (experienceDTOs == null) {
            return new ArrayList<>();
        }

        List<Experience> experiences = new ArrayList<>();

        for (ExperienceDTO experienceDTO : experienceDTOs) {
            experiences.add(fromCreateDTO(experienceDTO));
        }

        return experiences;
    }

    private static List<String> toStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }
}