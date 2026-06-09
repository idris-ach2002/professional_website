package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProfileMapper;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.mapper.TimelineMapper;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.util.List;

@Service
@Transactional
public class OwnerService {

    private final OwnerRepository rpOwner;
    private final WebsiteVersionRepository rpWebsiteVersion;

    public OwnerService(
            OwnerRepository rpOwner,
            WebsiteVersionRepository rpWebsiteVersion
    ) {
        this.rpOwner = rpOwner;
        this.rpWebsiteVersion = rpWebsiteVersion;
    }

    public void createOwner(OwnerRequestDTO ownerRequestDTO) {
        Owner owner = OwnerMapper.fromRequest(ownerRequestDTO);
        rpOwner.save(owner);
    }

    @Transactional(readOnly = true)
    public List<OwnerResponseDTO> getAllOwners() {
        return rpOwner.findAll()
                .stream()
                .map(OwnerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponseDTO getOwnerById(Long ownerId) {
        Owner owner = findOwnerById(ownerId);
        return OwnerMapper.toResponse(owner);
    }

    public void updateOwner(Long ownerId, OwnerRequestDTO ownerRequestDTO) {
        Owner owner = findOwnerById(ownerId);
        OwnerMapper.updateEntityFromRequest(owner, ownerRequestDTO);
        rpOwner.save(owner);
    }

    public void deleteOwner(Long ownerId) {
        Owner owner = findOwnerById(ownerId);
        rpOwner.delete(owner);
    }

    /**
     * Backward-compatible route: replaces the profile of the currently active version.
     */
    public void createOrReplaceProfile(Long ownerId, ProfileRequestDTO profileRequestDTO) {
        Owner owner = findOwnerById(ownerId);
        WebsiteVersion activeVersion = getOrCreateActiveVersion(owner);

        Profile profile = ProfileMapper.fromRequest(profileRequestDTO);
        activeVersion.attachProfile(profile);

        rpWebsiteVersion.save(activeVersion);
    }

    /**
     * Backward-compatible route: replaces the timeline of the currently active version.
     */
    public void createOrReplaceTimeline(Long ownerId, TimelineRequestDTO timelineRequestDTO) {
        Owner owner = findOwnerById(ownerId);
        WebsiteVersion activeVersion = getOrCreateActiveVersion(owner);

        Timeline timeline = TimelineMapper.fromRequest(timelineRequestDTO);
        activeVersion.attachTimeline(timeline);

        rpWebsiteVersion.save(activeVersion);
    }

    /**
     * Backward-compatible route: adds the project to the currently active version.
     */
    public void addProjectToOwner(Long ownerId, ProjectRequestDTO projectRequestDTO) {
        Owner owner = findOwnerById(ownerId);
        WebsiteVersion activeVersion = getOrCreateActiveVersion(owner);

        Project project = ProjectMapper.fromRequest(projectRequestDTO);
        activeVersion.addProject(project);

        rpWebsiteVersion.save(activeVersion);
    }

    private WebsiteVersion getOrCreateActiveVersion(Owner owner) {
        return owner.getActiveWebsiteVersion()
                .orElseGet(() -> createDefaultActiveVersion(owner));
    }

    private WebsiteVersion createDefaultActiveVersion(Owner owner) {
        WebsiteVersion version = WebsiteVersion.builder()
                .versionTag("v1")
                .label("Version initiale")
                .description("Version créée automatiquement pour compatibilité avec les anciennes routes manager.")
                .active(true)
                .published(true)
                .owner(owner)
                .build();

        owner.getWebsiteVersions().add(version);
        return rpWebsiteVersion.save(version);
    }

    private Owner findOwnerById(Long ownerId) {
        return rpOwner.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }
}
