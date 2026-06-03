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
                project.getDisplayOrder());
    }

    public static Project fromCreateDTO(ProjectDTO projectDTO) {
        if (projectDTO == null) {
            return null;
        }

        Project project = new Project();

        setProjectProperties(project, projectDTO);

        return project;
    }

    public static void updateEntityFromDTO(Project project, ProjectDTO projectDTO) {
        if (project == null || projectDTO == null) {
            return;
        }

        setProjectProperties(project, projectDTO);
    }

    private static void setProjectProperties(Project project, ProjectDTO projectDTO){
        project.setTitle(projectDTO.title());
        project.setSubtitle(projectDTO.subtitle());
        project.setShortDescription(projectDTO.shortDescription());
        project.setDescription(projectDTO.description());
        project.setStartDate(projectDTO.startDate());
        project.setEndDate(projectDTO.endDate());
        project.setImageUrl(projectDTO.imageUrl());
        project.setDemoUrl(projectDTO.demoUrl());
        project.setGithubUrl(projectDTO.githubUrl());
        project.setDocumentationUrl(projectDTO.documentationUrl());
        project.setStacks(toStringList(projectDTO.stacks()));
        project.setFeatures(toStringList(projectDTO.features()));
        project.setLinks(ProjectLinkMapper.fromDTOList(projectDTO.links()));
        project.setDisplayOrder(projectDTO.displayOrder());
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

    public static List<Project> fromDTOList(List<ProjectDTO> projectDTOs) {
        if (projectDTOs == null) {
            return new ArrayList<>();
        }

        List<Project> projects = new ArrayList<>();

        for (ProjectDTO projectDTO : projectDTOs) {
            projects.add(fromCreateDTO(projectDTO));
        }

        return projects;
    }

    private static List<String> toStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }
}