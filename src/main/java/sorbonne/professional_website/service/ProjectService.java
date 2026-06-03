package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.repository.ProjectRepository;

import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository rpProject;

    public ProjectService(ProjectRepository rpProject) {
        this.rpProject = rpProject;
    }

    public void createProject(ProjectRequestDTO projectRequestDTO) {
        Project project = ProjectMapper.fromRequest(projectRequestDTO);
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
