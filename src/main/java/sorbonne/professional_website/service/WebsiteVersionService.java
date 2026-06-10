package sorbonne.professional_website.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProfileMapper;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.mapper.TimelineMapper;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.ProjectRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class WebsiteVersionService {

    private final OwnerRepository rpOwner;
    private final WebsiteVersionRepository rpWebsiteVersion;
    private final ProjectRepository rpProject;

    public WebsiteVersionService(
            OwnerRepository rpOwner,
            WebsiteVersionRepository rpWebsiteVersion,
            ProjectRepository rpProject
    ) {
        this.rpOwner = rpOwner;
        this.rpWebsiteVersion = rpWebsiteVersion;
        this.rpProject = rpProject;
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


        WebsiteVersion savedVersion = rpWebsiteVersion.save(version);
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    /**
     * Creates a new version by copying every content block from another version of the same owner.
     * Metadata passed in versionDTO overrides the copied metadata. Content fields in versionDTO are intentionally ignored:
     * this endpoint is made to duplicate profile, timeline and projects without forcing the admin to retype them.
     */
    public WebsiteVersionResponseDTO createVersionFromExistingVersion(
            Long ownerId,
            Long sourceVersionId,
            WebsiteVersionRequestDTO versionDTO
    ) {
        Owner owner = lockOwner(ownerId);
        WebsiteVersion sourceVersion = findVersionByOwner(ownerId, sourceVersionId);

        boolean shouldActivate = shouldActivateCreatedVersion(ownerId, versionDTO);

        if (shouldActivate) {
            rpWebsiteVersion.deactivateAllByOwnerId(ownerId);
        }

        WebsiteVersion copiedVersion = WebsiteVersion.builder()
                .versionTag(defaultIfBlank(versionDTO != null ? versionDTO.versionTag() : null, buildDefaultTag(owner)))
                .label(defaultIfBlank(versionDTO != null ? versionDTO.label() : null, sourceVersion.getLabel() + " — copie"))
                .description(versionDTO != null ? versionDTO.description() : sourceVersion.getDescription())
                .active(shouldActivate)
                .published(versionDTO != null && versionDTO.published() != null ? versionDTO.published() : sourceVersion.getPublished())
                .owner(owner)
                .build();

        copiedVersion.attachProfile(copyProfile(sourceVersion.getProfile()));
        copiedVersion.attachTimeline(copyTimeline(sourceVersion.getTimeline()));
        copiedVersion.clearAndAttachProjects(copyProjects(sourceVersion.getProjects()));


        return WebsiteVersionMapper.toResponse(rpWebsiteVersion.save(copiedVersion));
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

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjects(Long ownerId, Long versionId) {
        findVersionByOwner(ownerId, versionId);

        return rpProject.findByWebsiteVersion_IdOrderByDisplayOrderAscStartDateDesc(versionId)
                .stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getProject(Long ownerId, Long versionId, Long projectId) {
        findVersionByOwner(ownerId, versionId);
        return ProjectMapper.toResponse(findProjectByVersion(versionId, projectId));
    }

    public ProjectResponseDTO updateProject(
            Long ownerId,
            Long versionId,
            Long projectId,
            ProjectRequestDTO projectRequestDTO
    ) {
        lockOwner(ownerId);
        findVersionByOwner(ownerId, versionId);

        Project project = findProjectByVersion(versionId, projectId);
        ProjectMapper.updateEntityFromRequest(project, projectRequestDTO);

        return ProjectMapper.toResponse(rpProject.save(project));
    }

    public void deleteProject(Long ownerId, Long versionId, Long projectId) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        Project project = findProjectByVersion(versionId, projectId);

        version.getProjects().remove(project);
        rpProject.delete(project);
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

    private Project findProjectByVersion(Long versionId, Long projectId) {
        return rpProject.findByIdAndWebsiteVersion_Id(projectId, versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
    }

    private Profile copyProfile(Profile source) {
        if (source == null) {
            return null;
        }

        return Profile.builder()
                .title(source.getTitle())
                .subtitle(source.getSubtitle())
                .headline(source.getHeadline())
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .location(source.getLocation())
                .availability(source.getAvailability())
                .profileImageUrl(source.getProfileImageUrl())
                .logoUrl(source.getLogoUrl())
                .cvUrl(source.getCvUrl())
                .portfolioUrl(source.getPortfolioUrl())
                .build();
    }

    private Timeline copyTimeline(Timeline source) {
        if (source == null) {
            return null;
        }

        Timeline copiedTimeline = Timeline.builder()
                .title(source.getTitle())
                .description(source.getDescription())
                .build();

        if (source.getExperiences() != null) {
            for (Experience sourceExperience : source.getExperiences()) {
                Experience copiedExperience = Experience.builder()
                        .category(sourceExperience.getCategory())
                        .title(sourceExperience.getTitle())
                        .organization(sourceExperience.getOrganization())
                        .location(sourceExperience.getLocation())
                        .summary(sourceExperience.getSummary())
                        .description(sourceExperience.getDescription())
                        .startDate(sourceExperience.getStartDate())
                        .endDate(sourceExperience.getEndDate())
                        .currentPosition(sourceExperience.isCurrentPosition())
                        .imageUrl(sourceExperience.getImageUrl())
                        .websiteUrl(sourceExperience.getWebsiteUrl())
                        .skills(sourceExperience.getSkills() == null ? new ArrayList<>() : new ArrayList<>(sourceExperience.getSkills()))
                        .displayOrder(sourceExperience.getDisplayOrder())
                        .timeline(copiedTimeline)
                        .build();

                copiedTimeline.getExperiences().add(copiedExperience);
            }
        }

        return copiedTimeline;
    }

    private List<Project> copyProjects(List<Project> sourceProjects) {
        if (sourceProjects == null) {
            return new ArrayList<>();
        }

        List<Project> copiedProjects = new ArrayList<>();

        for (Project sourceProject : sourceProjects) {
            Project copiedProject = Project.builder()
                    .title(sourceProject.getTitle())
                    .subtitle(sourceProject.getSubtitle())
                    .shortDescription(sourceProject.getShortDescription())
                    .description(sourceProject.getDescription())
                    .status(sourceProject.getStatus())
                    .startDate(sourceProject.getStartDate())
                    .endDate(sourceProject.getEndDate())
                    .imageUrl(sourceProject.getImageUrl())
                    .demoUrl(sourceProject.getDemoUrl())
                    .githubUrl(sourceProject.getGithubUrl())
                    .documentationUrl(sourceProject.getDocumentationUrl())
                    .stacks(sourceProject.getStacks() == null ? new ArrayList<>() : new ArrayList<>(sourceProject.getStacks()))
                    .features(sourceProject.getFeatures() == null ? new ArrayList<>() : new ArrayList<>(sourceProject.getFeatures()))
                    .links(copyProjectLinks(sourceProject.getLinks()))
                    .featured(sourceProject.getFeatured())
                    .published(sourceProject.getPublished())
                    .displayOrder(sourceProject.getDisplayOrder())
                    .build();

            copiedProjects.add(copiedProject);
        }

        return copiedProjects;
    }

    private List<Project.ProjectLink> copyProjectLinks(List<Project.ProjectLink> sourceLinks) {
        if (sourceLinks == null) {
            return new ArrayList<>();
        }

        List<Project.ProjectLink> copiedLinks = new ArrayList<>();

        for (Project.ProjectLink sourceLink : sourceLinks) {
            copiedLinks.add(Project.ProjectLink.builder()
                    .type(sourceLink.getType())
                    .label(sourceLink.getLabel())
                    .url(sourceLink.getUrl())
                    .build());
        }

        return copiedLinks;
    }

    private String buildDefaultTag(Owner owner) {
        if (owner == null || owner.getOwnerId() == null) {
            return "v1";
        }

        long nextVersionNumber = rpWebsiteVersion.countByOwnerOwnerId(owner.getOwnerId()) + 1;
        return "v" + nextVersionNumber;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}
