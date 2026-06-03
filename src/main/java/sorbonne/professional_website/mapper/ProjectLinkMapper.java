package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.ProjectLinkDTO;
import sorbonne.professional_website.entity.Project;

import java.util.ArrayList;
import java.util.List;

public final class ProjectLinkMapper {

    private ProjectLinkMapper() {
    }

    public static ProjectLinkDTO toDTO(Project.ProjectLink projectLink) {
        if (projectLink == null) {
            return null;
        }

        return new ProjectLinkDTO(
                projectLink.getType(),
                projectLink.getLabel(),
                projectLink.getUrl()
        );
    }

    public static Project.ProjectLink fromCreateDTO(ProjectLinkDTO projectLinkDTO) {
        if (projectLinkDTO == null) {
            return null;
        }

        Project.ProjectLink projectLink = new Project.ProjectLink();

        projectLink.setType(projectLinkDTO.type());
        projectLink.setLabel(projectLinkDTO.label());
        projectLink.setUrl(projectLinkDTO.url());

        return projectLink;
    }

    public static List<ProjectLinkDTO> toDTOList(List<Project.ProjectLink> projectLinks) {
        if (projectLinks == null) {
            return List.of();
        }

        List<ProjectLinkDTO> projectLinkDTOs = new ArrayList<>();

        for (Project.ProjectLink projectLink : projectLinks) {
            projectLinkDTOs.add(toDTO(projectLink));
        }

        return projectLinkDTOs;
    }

    public static List<Project.ProjectLink> fromDTOList(List<ProjectLinkDTO> projectLinkDTOs) {
        if (projectLinkDTOs == null) {
            return new ArrayList<>();
        }

        List<Project.ProjectLink> projectLinks = new ArrayList<>();

        for (ProjectLinkDTO projectLinkDTO : projectLinkDTOs) {
            projectLinks.add(fromCreateDTO(projectLinkDTO));
        }

        return projectLinks;
    }
}