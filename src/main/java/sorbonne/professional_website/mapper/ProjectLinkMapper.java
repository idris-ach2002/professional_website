package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProjectLinkRequestDTO;
import sorbonne.professional_website.dto.response.ProjectLinkResponseDTO;
import sorbonne.professional_website.entity.Project;

import java.util.ArrayList;
import java.util.List;

public final class ProjectLinkMapper {

    private ProjectLinkMapper() {
    }

    public static ProjectLinkResponseDTO toResponse(Project.ProjectLink projectLink) {
        if (projectLink == null) {
            return null;
        }

        return new ProjectLinkResponseDTO(
                projectLink.getType(),
                projectLink.getLabel(),
                projectLink.getUrl()
        );
    }

    public static Project.ProjectLink fromRequest(ProjectLinkRequestDTO projectLinkDTO) {
        if (projectLinkDTO == null) {
            return null;
        }

        Project.ProjectLink projectLink = new Project.ProjectLink();

        projectLink.setType(projectLinkDTO.type());
        projectLink.setLabel(projectLinkDTO.label());
        projectLink.setUrl(projectLinkDTO.url());

        return projectLink;
    }

    public static List<ProjectLinkResponseDTO> toResponseList(List<Project.ProjectLink> projectLinks) {
        if (projectLinks == null) {
            return List.of();
        }

        List<ProjectLinkResponseDTO> projectLinkDTOs = new ArrayList<>();

        for (Project.ProjectLink projectLink : projectLinks) {
            projectLinkDTOs.add(toResponse(projectLink));
        }

        return projectLinkDTOs;
    }

    public static List<Project.ProjectLink> fromRequestList(List<ProjectLinkRequestDTO> projectLinkDTOs) {
        if (projectLinkDTOs == null) {
            return new ArrayList<>();
        }

        List<Project.ProjectLink> projectLinks = new ArrayList<>();

        for (ProjectLinkRequestDTO projectLinkDTO : projectLinkDTOs) {
            Project.ProjectLink projectLink = fromRequest(projectLinkDTO);

            if (projectLink != null) {
                projectLinks.add(projectLink);
            }
        }

        return projectLinks;
    }
}
