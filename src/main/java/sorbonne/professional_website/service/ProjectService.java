package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.ProjectDTO;
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

    public void createProject(ProjectDTO projectDTO) {
        Project project = ProjectMapper.fromCreateDTO(projectDTO);
        rpProject.save(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return rpProject.findAll()
                .stream()
                .map(ProjectMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long projectId) {
        Project project = findProjectById(projectId);
        return ProjectMapper.toDTO(project);
    }

    public void updateProject(Long projectId, ProjectDTO projectDTO) {
        Project project = findProjectById(projectId);

        ProjectMapper.updateEntityFromDTO(project, projectDTO);

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