package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ExperienceRequestDTO;
import sorbonne.professional_website.dto.response.ExperienceResponseDTO;
import sorbonne.professional_website.entity.Experience;

import java.util.ArrayList;
import java.util.List;

public final class ExperienceMapper {

    private ExperienceMapper() {
    }

    public static ExperienceResponseDTO toResponse(Experience experience) {
        if (experience == null) {
            return null;
        }

        return new ExperienceResponseDTO(
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
                copyStringList(experience.getSkills()),
                experience.getDisplayOrder()
        );
    }

    public static Experience fromRequest(ExperienceRequestDTO experienceDTO) {
        if (experienceDTO == null) {
            return null;
        }

        Experience experience = new Experience();
        setExperienceProperties(experience, experienceDTO);

        return experience;
    }

    public static void updateEntityFromRequest(Experience experience, ExperienceRequestDTO experienceDTO) {
        if (experience == null || experienceDTO == null) {
            return;
        }

        setExperienceProperties(experience, experienceDTO);
    }

    public static List<ExperienceResponseDTO> toResponseList(List<Experience> experiences) {
        if (experiences == null) {
            return List.of();
        }

        List<ExperienceResponseDTO> experienceDTOs = new ArrayList<>();

        for (Experience experience : experiences) {
            experienceDTOs.add(toResponse(experience));
        }

        return experienceDTOs;
    }

    public static List<Experience> fromRequestList(List<ExperienceRequestDTO> experienceDTOs) {
        if (experienceDTOs == null) {
            return new ArrayList<>();
        }

        List<Experience> experiences = new ArrayList<>();

        for (ExperienceRequestDTO experienceDTO : experienceDTOs) {
            Experience experience = fromRequest(experienceDTO);

            if (experience != null) {
                experiences.add(experience);
            }
        }

        return experiences;
    }

    private static void setExperienceProperties(Experience experience, ExperienceRequestDTO experienceDTO) {
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
        experience.setSkills(copyStringList(experienceDTO.skills()));
        experience.setDisplayOrder(experienceDTO.displayOrder());
    }

    private static List<String> copyStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }
}
