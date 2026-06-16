package sorbonne.professional_website.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.request.PortfolioRestoreRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioBackupResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioHealthReportResponseDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.service.WebsiteVersionService;

import java.util.List;

@RestController
@RequestMapping("/manager/{ownerId}/versions")
public class WebsiteVersionAdminController {

    private final WebsiteVersionService srvWebsiteVersion;

    public WebsiteVersionAdminController(WebsiteVersionService srvWebsiteVersion) {
        this.srvWebsiteVersion = srvWebsiteVersion;
    }

    @GetMapping
    public ResponseEntity<List<WebsiteVersionResponseDTO>> getVersionsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(srvWebsiteVersion.getVersionsByOwner(ownerId));
    }

    @GetMapping("/active")
    public ResponseEntity<WebsiteVersionResponseDTO> getActiveVersionByOwner(
            @PathVariable Long ownerId
    ) {
        return ResponseEntity.ok(
                srvWebsiteVersion.getActiveVersionByOwner(ownerId)
        );
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<WebsiteVersionResponseDTO> getVersion(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.getVersion(ownerId, versionId));
    }


    @GetMapping("/{versionId}/health")
    public ResponseEntity<PortfolioHealthReportResponseDTO> getHealthReport(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.getHealthReport(ownerId, versionId));
    }

    @GetMapping("/{versionId}/publish-validation")
    public ResponseEntity<PortfolioHealthReportResponseDTO> validateBeforePublish(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.validateBeforePublish(ownerId, versionId));
    }

    @PutMapping("/{versionId}/activate-validated")
    public ResponseEntity<WebsiteVersionResponseDTO> activateVersionAfterValidation(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.activateVersionAfterValidation(ownerId, versionId));
    }

    @PostMapping("/{versionId}/backup/export")
    public ResponseEntity<PortfolioBackupResponseDTO> exportVersionBackup(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.exportVersionBackup(ownerId, versionId));
    }

    @PostMapping("/backup/restore")
    public ResponseEntity<WebsiteVersionResponseDTO> restoreVersionBackup(
            @PathVariable Long ownerId,
            @RequestBody @Valid PortfolioRestoreRequestDTO requestDTO
    ) {
        WebsiteVersionResponseDTO restoredVersion = srvWebsiteVersion.restoreVersionBackup(ownerId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(restoredVersion);
    }

    @PostMapping
    public ResponseEntity<WebsiteVersionResponseDTO> createVersion(
            @PathVariable Long ownerId,
            @RequestBody @Valid WebsiteVersionRequestDTO versionRequestDTO
    ) {
        WebsiteVersionResponseDTO createdVersion = srvWebsiteVersion.createVersion(ownerId, versionRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVersion);
    }

    @PostMapping("/from/{sourceVersionId}")
    public ResponseEntity<WebsiteVersionResponseDTO> createVersionFromExistingVersion(
            @PathVariable Long ownerId,
            @PathVariable Long sourceVersionId,
            @RequestBody @Valid WebsiteVersionRequestDTO versionRequestDTO
    ) {
        WebsiteVersionResponseDTO createdVersion = srvWebsiteVersion
                .createVersionFromExistingVersion(ownerId, sourceVersionId, versionRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVersion);
    }

    @PutMapping("/{versionId}")
    public ResponseEntity<WebsiteVersionResponseDTO> updateVersion(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody @Valid WebsiteVersionRequestDTO versionRequestDTO
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.updateVersion(ownerId, versionId, versionRequestDTO));
    }

    @PutMapping("/{versionId}/activate")
    public ResponseEntity<WebsiteVersionResponseDTO> activateVersion(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.activateVersion(ownerId, versionId));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        srvWebsiteVersion.deleteVersion(ownerId, versionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{versionId}/profile")
    public ResponseEntity<WebsiteVersionResponseDTO> createOrReplaceProfile(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody @Valid ProfileRequestDTO profileRequestDTO
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.createOrReplaceProfile(ownerId, versionId, profileRequestDTO));
    }

    @PutMapping("/{versionId}/timeline")
    public ResponseEntity<WebsiteVersionResponseDTO> createOrReplaceTimeline(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody @Valid TimelineRequestDTO timelineRequestDTO
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.createOrReplaceTimeline(ownerId, versionId, timelineRequestDTO));
    }

    @PostMapping("/{versionId}/projects")
    public ResponseEntity<WebsiteVersionResponseDTO> addProject(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody @Valid ProjectRequestDTO projectRequestDTO
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(srvWebsiteVersion.addProject(ownerId, versionId, projectRequestDTO));
    }

    @GetMapping("/{versionId}/projects")
    public ResponseEntity<List<ProjectResponseDTO>> getProjects(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.getProjects(ownerId, versionId));
    }

    @GetMapping("/{versionId}/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getProject(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.getProject(ownerId, versionId, projectId));
    }

    @PutMapping("/{versionId}/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectRequestDTO projectRequestDTO
    ) {
        return ResponseEntity.ok(srvWebsiteVersion.updateProject(ownerId, versionId, projectId, projectRequestDTO));
    }

    @DeleteMapping("/{versionId}/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @PathVariable Long projectId
    ) {
        srvWebsiteVersion.deleteProject(ownerId, versionId, projectId);
        return ResponseEntity.noContent().build();
    }
}
