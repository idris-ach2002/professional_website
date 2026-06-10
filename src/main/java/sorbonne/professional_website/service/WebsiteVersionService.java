package sorbonne.professional_website.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.*;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.util.List;

@Service
@Transactional
public class WebsiteVersionService {

    private final OwnerRepository rpOwner;
    private final WebsiteVersionRepository rpWebsiteVersion;

    public WebsiteVersionService(
            OwnerRepository rpOwner,
            WebsiteVersionRepository rpWebsiteVersion
    ) {
        this.rpOwner = rpOwner;
        this.rpWebsiteVersion = rpWebsiteVersion;
    }

    @Transactional(readOnly = true)
    public List<WebsiteVersionResponseDTO> getVersionsByOwner(Long ownerId) {
        ensureOwnerExists(ownerId);

        return rpWebsiteVersion.findByOwnerOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(WebsiteVersionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WebsiteVersionResponseDTO getActiveVersionByOwner(Long ownerId) {
        WebsiteVersion activeVersion = rpWebsiteVersion
                .findByOwnerOwnerIdAndActiveTrue(ownerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active website version found for owner id: " + ownerId
                ));

        return WebsiteVersionMapper.toResponse(activeVersion);
    }

    @Transactional(readOnly = true)
    public WebsiteVersionResponseDTO getVersion(Long ownerId, Long versionId) {
        return WebsiteVersionMapper.toResponse(findVersionByOwner(ownerId, versionId));
    }

    private boolean shouldActivateCreatedVersion(Long ownerId, WebsiteVersionRequestDTO versionDTO) {
        if (versionDTO != null && Boolean.TRUE.equals(versionDTO.active())) {
            return true;
        }

        return !rpWebsiteVersion.existsByOwnerOwnerIdAndActiveTrue(ownerId);
    }

    public WebsiteVersionResponseDTO createVersion(Long ownerId, WebsiteVersionRequestDTO versionDTO) {
        Owner owner = lockOwner(ownerId);

        boolean shouldActivate = shouldActivateCreatedVersion(ownerId, versionDTO);

        if (shouldActivate) {
            rpWebsiteVersion.deactivateAllByOwnerId(ownerId);
        }

        WebsiteVersion version = WebsiteVersionMapper.fromRequest(versionDTO);
        version.setVersionTag(defaultIfBlank(version.getVersionTag(), buildDefaultTag(owner)));
        version.setLabel(defaultIfBlank(version.getLabel(), "Nouvelle version"));
        version.setDescription(versionDTO != null ? versionDTO.description() : null);
        version.setActive(shouldActivate);
        version.setPublished(versionDTO != null && versionDTO.published() != null ? versionDTO.published() : shouldActivate);
        version.setOwner(owner);

        owner.getWebsiteVersions().add(version);

        WebsiteVersion savedVersion = rpWebsiteVersion.save(version);
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    public WebsiteVersionResponseDTO updateVersion(Long ownerId, Long versionId, WebsiteVersionRequestDTO versionDTO) {
        lockOwner(ownerId);

        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        WebsiteVersionMapper.updateEntityFromRequest(version, versionDTO);

        if (version.getVersionTag() == null || version.getVersionTag().isBlank()) {
            version.setVersionTag("v" + versionId);
        }

        if (version.getLabel() == null || version.getLabel().isBlank()) {
            version.setLabel("Version " + version.getVersionTag());
        }

        if (versionDTO != null && Boolean.TRUE.equals(versionDTO.active())) {
            return activateVersion(ownerId, versionId);
        }

        return WebsiteVersionMapper.toResponse(rpWebsiteVersion.save(version));
    }

    public WebsiteVersionResponseDTO activateVersion(Long ownerId, Long versionId) {
        lockOwner(ownerId);

        WebsiteVersion version = findVersionByOwner(ownerId, versionId);

        rpWebsiteVersion.deactivateAllByOwnerId(ownerId);

        version.setActive(true);
        version.setPublished(true);

        return WebsiteVersionMapper.toResponse(rpWebsiteVersion.saveAndFlush(version));
    }

    public WebsiteVersionResponseDTO createOrReplaceProfile(
            Long ownerId,
            Long versionId,
            ProfileRequestDTO profileRequestDTO
    ) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);

        Profile profile = ProfileMapper.fromRequest(profileRequestDTO);
        version.attachProfile(profile);

        return WebsiteVersionMapper.toResponse(rpWebsiteVersion.save(version));
    }

    public WebsiteVersionResponseDTO createOrReplaceTimeline(
            Long ownerId,
            Long versionId,
            TimelineRequestDTO timelineRequestDTO
    ) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);

        Timeline timeline = TimelineMapper.fromRequest(timelineRequestDTO);
        version.attachTimeline(timeline);

        return WebsiteVersionMapper.toResponse(rpWebsiteVersion.save(version));
    }

    public WebsiteVersionResponseDTO addProject(
            Long ownerId,
            Long versionId,
            ProjectRequestDTO projectRequestDTO
    ) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);

        Project project = ProjectMapper.fromRequest(projectRequestDTO);
        version.addProject(project);

        return WebsiteVersionMapper.toResponse(rpWebsiteVersion.save(version));
    }

    public void deleteVersion(Long ownerId, Long versionId) {
        lockOwner(ownerId);

        WebsiteVersion version = findVersionByOwner(ownerId, versionId);

        if (Boolean.TRUE.equals(version.getActive())) {
            throw new IllegalStateException("Impossible de supprimer la version active. Activez une autre version avant suppression.");
        }

        rpWebsiteVersion.delete(version);
    }



    private Owner lockOwner(Long ownerId) {
        return rpOwner.lockByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }

    private void ensureOwnerExists(Long ownerId) {
        if (!rpOwner.existsById(ownerId)) {
            throw new ResourceNotFoundException("Owner");
        }
    }

    private WebsiteVersion findVersionByOwner(Long ownerId, Long versionId) {
        return rpWebsiteVersion.findByIdAndOwnerOwnerId(versionId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteVersion"));
    }

    private String buildDefaultTag(Owner owner) {
        int nextVersionNumber = owner.getWebsiteVersions() == null ? 1 : owner.getWebsiteVersions().size() + 1;
        return "v" + nextVersionNumber;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}
