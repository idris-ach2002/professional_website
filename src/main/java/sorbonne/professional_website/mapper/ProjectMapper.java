package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.Project;

import java.util.ArrayList;
import java.util.List;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectResponseDTO toResponse(Project project) {
        if (project == null) {
            return null;
        }

        return new ProjectResponseDTO(
                project.getId(),
                project.getTitle(),
                project.getSubtitle(),
                project.getShortDescription(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getImageUrl(),
                project.getDemoUrl(),
                project.getGithubUrl(),
                project.getDocumentationUrl(),
                copyStringList(project.getStacks()),
                copyStringList(project.getFeatures()),
                ProjectLinkMapper.toResponseList(project.getLinks()),
                project.isFeatured(),
                project.isPublished(),
                project.getDisplayOrder()
        );
    }

    public static Project fromRequest(ProjectRequestDTO projectDTO) {
        if (projectDTO == null) {
            return null;
        }

        Project project = new Project();
        setProjectProperties(project, projectDTO);

        return project;
    }

    public static void updateEntityFromRequest(Project project, ProjectRequestDTO projectDTO) {
        if (project == null || projectDTO == null) {
            return;
        }

        setProjectProperties(project, projectDTO);
    }

    public static List<ProjectResponseDTO> toResponseList(List<Project> projects) {
        if (projects == null) {
            return List.of();
        }

        List<ProjectResponseDTO> projectDTOs = new ArrayList<>();

        for (Project project : projects) {
            projectDTOs.add(toResponse(project));
        }

        return projectDTOs;
    }

    public static List<Project> fromRequestList(List<ProjectRequestDTO> projectDTOs) {
        if (projectDTOs == null) {
            return new ArrayList<>();
        }

        List<Project> projects = new ArrayList<>();

        for (ProjectRequestDTO projectDTO : projectDTOs) {
            Project project = fromRequest(projectDTO);

            if (project != null) {
                projects.add(project);
            }
        }

        return projects;
    }

    private static void setProjectProperties(Project project, ProjectRequestDTO projectDTO) {
        project.setTitle(projectDTO.title());
        project.setSubtitle(projectDTO.subtitle());
        project.setShortDescription(projectDTO.shortDescription());
        project.setDescription(projectDTO.description());
        project.setStatus(projectDTO.status());
        project.setStartDate(projectDTO.startDate());
        project.setEndDate(projectDTO.endDate());
        project.setImageUrl(projectDTO.imageUrl());
        project.setDemoUrl(projectDTO.demoUrl());
        project.setGithubUrl(projectDTO.githubUrl());
        project.setDocumentationUrl(projectDTO.documentationUrl());
        project.setStacks(copyStringList(projectDTO.stacks()));
        project.setFeatures(copyStringList(projectDTO.features()));
        project.setLinks(ProjectLinkMapper.fromRequestList(projectDTO.links()));
        project.setFeatured(projectDTO.featured());
        project.setPublished(projectDTO.published());
        project.setDisplayOrder(projectDTO.displayOrder());
    }

    private static List<String> copyStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }
}
