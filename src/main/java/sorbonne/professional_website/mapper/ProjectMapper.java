package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.ProjectDTO;
import sorbonne.professional_website.entity.Project;

import java.util.ArrayList;
import java.util.List;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectDTO toDTO(Project project) {
        if (project == null) {
            return null;
        }

        return new ProjectDTO(
                project.getTitle(),
                project.getSubtitle(),
                project.getShortDescription(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getImageUrl(),
                project.getDemoUrl(),
                project.getGithubUrl(),
                project.getDocumentationUrl(),
                project.getStacks(),
                project.getFeatures(),
                ProjectLinkMapper.toDTOList(project.getLinks()),
                project.getDisplayOrder()
        );
    }

    public static List<ProjectDTO> toDTOList(List<Project> projects) {
        if (projects == null) {
            return List.of();
        }

        List<ProjectDTO> projectDTOs = new ArrayList<>();

        for (Project project : projects) {
            projectDTOs.add(toDTO(project));
        }

        return projectDTOs;
    }
}