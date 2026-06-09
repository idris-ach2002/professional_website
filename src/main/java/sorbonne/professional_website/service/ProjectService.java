package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.repository.ProjectRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository rpProject;
    private final WebsiteVersionRepository rpWebsiteVersion;

    public ProjectService(
            ProjectRepository rpProject,
            WebsiteVersionRepository rpWebsiteVersion
    ) {
        this.rpProject = rpProject;
        this.rpWebsiteVersion = rpWebsiteVersion;
    }

    public void createProject(ProjectRequestDTO projectRequestDTO) {
        if (projectRequestDTO.websiteVersionId() == null) {
            throw new IllegalArgumentException("websiteVersionId est obligatoire pour créer un projet via /api/projects. Utilisez /manager/{ownerId}/versions/{versionId}/projects si vous avez déjà le ownerId.");
        }

        WebsiteVersion version = rpWebsiteVersion.findById(projectRequestDTO.websiteVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteVersion"));

        Project project = ProjectMapper.fromRequest(projectRequestDTO);
        project.setWebsiteVersion(version);

        rpProject.save(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAllProjects() {
        return rpProject.findAll()
                .stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long projectId) {
        Project project = findProjectById(projectId);
        return ProjectMapper.toResponse(project);
    }

    public void updateProject(Long projectId, ProjectRequestDTO projectRequestDTO) {
        Project project = findProjectById(projectId);
        ProjectMapper.updateEntityFromRequest(project, projectRequestDTO);
        rpProject.save(project);
    }

    public void deleteProject(Long projectId) {
        Project project = findProjectById(projectId);
        rpProject.delete(project);
    }

    private Project findProjectById(Long projectId) {
        return rpProject.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
    }
}
