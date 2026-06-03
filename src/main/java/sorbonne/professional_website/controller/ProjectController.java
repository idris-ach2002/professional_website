package sorbonne.professional_website.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.service.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService srvProject;

    public ProjectController(ProjectService srvProject) {
        this.srvProject = srvProject;
    }

    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody @Valid ProjectRequestDTO projectRequestDTO) {
        srvProject.createProject(projectRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects() {
        return ResponseEntity.ok(srvProject.getAllProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(srvProject.getProjectById(projectId));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<Void> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectRequestDTO projectRequestDTO
    ) {
        srvProject.updateProject(projectId, projectRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        srvProject.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
