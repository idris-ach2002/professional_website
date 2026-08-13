package sorbonne.professional_website.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.cache.PortfolioChangePublisher;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.PortfolioRestoreRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioBackupResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioHealthReportResponseDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.publication.PublicationStatus;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.mapper.ProfileMapper;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.mapper.TimelineMapper;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;
import sorbonne.professional_website.upload.StorageService;
import sorbonne.professional_website.upload.StoredFile;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.ProjectRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@Transactional
public class WebsiteVersionService {

    private final OwnerRepository rpOwner;
    private final WebsiteVersionRepository rpWebsiteVersion;
    private final ProjectRepository rpProject;
    private final StorageService storageService;
    private final PortfolioChangePublisher changePublisher;
    private final PortfolioHealthEvaluator healthEvaluator;
    private final PortfolioBackupCodec backupCodec;
    private final WebsiteVersionCloner versionCloner;

    public WebsiteVersionService(
            OwnerRepository rpOwner,
            WebsiteVersionRepository rpWebsiteVersion,
            ProjectRepository rpProject,
            StorageService storageService,
            PortfolioChangePublisher changePublisher,
            PortfolioHealthEvaluator healthEvaluator,
            PortfolioBackupCodec backupCodec,
            WebsiteVersionCloner versionCloner
    ) {
        this.rpOwner = rpOwner;
        this.rpWebsiteVersion = rpWebsiteVersion;
        this.rpProject = rpProject;
        this.storageService = storageService;
        this.changePublisher = changePublisher;
        this.healthEvaluator = healthEvaluator;
        this.backupCodec = backupCodec;
        this.versionCloner = versionCloner;
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
        version.setPublicationStatus(Boolean.TRUE.equals(version.getPublished())
                ? (shouldActivate ? PublicationStatus.PUBLISHED : PublicationStatus.READY)
                : PublicationStatus.DRAFT);
        if (version.getPublicationStatus() == PublicationStatus.PUBLISHED) version.setPublishedAt(LocalDateTime.now());
        version.setOwner(owner);


        WebsiteVersion savedVersion = rpWebsiteVersion.save(version);
        changePublisher.changed(ownerId, "version-created");
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
                .published(versionDTO != null && versionDTO.published() != null ? versionDTO.published() : false)
                .publicationStatus(PublicationStatus.DRAFT)
                .owner(owner)
                .build();

        copiedVersion.attachProfile(versionCloner.copyProfile(sourceVersion.getProfile()));
        copiedVersion.attachTimeline(versionCloner.copyTimeline(sourceVersion.getTimeline()));
        copiedVersion.clearAndAttachProjects(versionCloner.copyProjects(sourceVersion.getProjects()));


        WebsiteVersion savedVersion = rpWebsiteVersion.save(copiedVersion);
        changePublisher.changed(ownerId, "version-copied");
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    public WebsiteVersionResponseDTO updateVersion(Long ownerId, Long versionId, long expectedRevision, WebsiteVersionRequestDTO versionDTO) {
        lockOwner(ownerId);

        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);
        WebsiteVersionMapper.updateEntityFromRequest(version, versionDTO);
        // Publication state is owned by PublicationService. Metadata/content edits cannot
        // silently publish or unpublish a version through the legacy boolean field.
        version.setPublished(version.getPublicationStatus() == PublicationStatus.PUBLISHED);
        if (version.getPublicationStatus() != PublicationStatus.PUBLISHED) version.setActive(false);

        if (version.getVersionTag() == null || version.getVersionTag().isBlank()) {
            version.setVersionTag("v" + versionId);
        }

        if (version.getLabel() == null || version.getLabel().isBlank()) {
            version.setLabel("Version " + version.getVersionTag());
        }

        if (versionDTO != null && Boolean.TRUE.equals(versionDTO.active())) {
            return activateLocked(ownerId, version);
        }

        version.bumpContentRevision();
        WebsiteVersion savedVersion = rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "version-updated");
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    public WebsiteVersionResponseDTO activateVersion(Long ownerId, Long versionId, long expectedRevision) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);
        return activateLocked(ownerId, version);
    }

    private WebsiteVersionResponseDTO activateLocked(Long ownerId, WebsiteVersion version) {
        Long versionId = version.getId();
        // Do not bulk-update the managed target itself: JPQL bulk updates bypass
        // the persistence context and could otherwise leave an already-active
        // target stale in memory while the database row becomes inactive.
        rpWebsiteVersion.deactivateOthersByOwnerId(ownerId, versionId);

        version.setActive(true);
        version.setPublished(true);
        version.setPublicationStatus(PublicationStatus.PUBLISHED);
        version.setScheduledAt(null);
        version.setPublishedAt(LocalDateTime.now());
        version.setPublicationError(null);
        version.bumpContentRevision();

        WebsiteVersion savedVersion = rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "version-activated");
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    public WebsiteVersionResponseDTO createOrReplaceProfile(
            Long ownerId,
            Long versionId,
            long expectedRevision,
            ProfileRequestDTO profileRequestDTO
    ) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);

        Profile profile = ProfileMapper.fromRequest(profileRequestDTO);
        version.attachProfile(profile);
        version.bumpContentRevision();

        WebsiteVersion savedVersion = rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "profile-updated");
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    public WebsiteVersionResponseDTO createOrReplaceTimeline(
            Long ownerId,
            Long versionId,
            long expectedRevision,
            TimelineRequestDTO timelineRequestDTO
    ) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);

        Timeline timeline = TimelineMapper.fromRequest(timelineRequestDTO);
        version.attachTimeline(timeline);
        version.bumpContentRevision();

        WebsiteVersion savedVersion = rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "timeline-updated");
        return WebsiteVersionMapper.toResponse(savedVersion);
    }

    public WebsiteVersionResponseDTO addProject(
            Long ownerId,
            Long versionId,
            long expectedRevision,
            ProjectRequestDTO projectRequestDTO
    ) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);

        requireUniqueProjectSlug(versionId, null, projectRequestDTO);
        Project project = ProjectMapper.fromRequest(projectRequestDTO);
        version.addProject(project);
        version.bumpContentRevision();

        WebsiteVersion savedVersion = rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "project-added");
        return WebsiteVersionMapper.toResponse(savedVersion);
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
            long expectedRevision,
            ProjectRequestDTO projectRequestDTO
    ) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);

        Project project = findProjectByVersion(versionId, projectId);
        requireUniqueProjectSlug(versionId, projectId, projectRequestDTO);
        ProjectMapper.updateEntityFromRequest(project, projectRequestDTO);

        Project savedProject = rpProject.save(project);
        version.bumpContentRevision();
        rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "project-updated");
        return ProjectMapper.toResponse(savedProject);
    }

    public void deleteProject(Long ownerId, Long versionId, Long projectId, long expectedRevision) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);
        Project project = findProjectByVersion(versionId, projectId);

        version.getProjects().remove(project);
        rpProject.delete(project);
        version.bumpContentRevision();
        rpWebsiteVersion.saveAndFlush(version);
        changePublisher.changed(ownerId, "project-deleted");
    }

    public void deleteVersion(Long ownerId, Long versionId, long expectedRevision) {
        lockOwner(ownerId);

        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);

        if (Boolean.TRUE.equals(version.getActive())) {
            throw new IllegalStateException("Impossible de supprimer la version active. Activez une autre version avant suppression.");
        }

        rpWebsiteVersion.delete(version);
        changePublisher.changed(ownerId, "version-deleted");
    }


    @Transactional(readOnly = true)
    public PortfolioHealthReportResponseDTO getHealthReport(Long ownerId, Long versionId) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        return healthEvaluator.evaluate(ownerId, versionId, version);
    }

    @Transactional(readOnly = true)
    public PortfolioHealthReportResponseDTO validateBeforePublish(Long ownerId, Long versionId) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        return healthEvaluator.evaluate(ownerId, versionId, version);
    }

    public WebsiteVersionResponseDTO activateVersionAfterValidation(Long ownerId, Long versionId, long expectedRevision) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        requireContentRevision(version, expectedRevision);
        PortfolioHealthReportResponseDTO report = healthEvaluator.evaluate(ownerId, versionId, version);
        if (!report.publishable()) {
            throw new IllegalStateException("Publication bloquée : corrige les erreurs critiques avant activation.");
        }
        return activateLocked(ownerId, version);
    }

    @Transactional(readOnly = true)
    public PortfolioBackupResponseDTO exportVersionBackup(Long ownerId, Long versionId) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        PortfolioBackupCodec.BackupArtifact artifact = backupCodec.encode(ownerId, version);
        String filename = artifact.filename();
        String json = artifact.json();
        byte[] zipBytes = artifact.zipBytes();
        StoredFile storedFile = storageService.storeBytes(filename, zipBytes);
        return new PortfolioBackupResponseDTO(
                true,
                filename,
                publicUrl(storedFile),
                json,
                zipBytes.length,
                LocalDateTime.now(),
                ownerId,
                versionId
        );
    }

    public WebsiteVersionResponseDTO restoreVersionBackup(Long ownerId, PortfolioRestoreRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new IllegalArgumentException("Backup vide.");
        }

        WebsiteVersionRequestDTO restoredRequest = requestDTO.version();
        if (restoredRequest == null && requestDTO.backupJson() != null && !requestDTO.backupJson().isBlank()) {
            restoredRequest = backupCodec.decodeVersionRequest(requestDTO.backupJson());
        }

        if (restoredRequest == null) {
            throw new IllegalArgumentException("Le backup ne contient aucune version restaurable.");
        }

        String label = defaultIfBlank(requestDTO.restoreLabel(), defaultIfBlank(restoredRequest.label(), "Version restaurée") + " — restaurée");
        WebsiteVersionRequestDTO finalRequest = new WebsiteVersionRequestDTO(
                defaultIfBlank(restoredRequest.versionTag(), buildDefaultTag(lockOwner(ownerId))) + "-restore",
                label,
                restoredRequest.description(),
                Boolean.TRUE.equals(requestDTO.active()),
                restoredRequest.published(),
                restoredRequest.prof(),
                restoredRequest.timeline(),
                restoredRequest.projects()
        );

        return createVersion(ownerId, finalRequest);
    }

    private String publicUrl(StoredFile storedFile) {
        if (storedFile.url() != null && !storedFile.url().isBlank()) {
            return storedFile.url();
        }
        try {
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/uploads/files/{filename}")
                    .buildAndExpand(storedFile.filename())
                    .toUriString();
        } catch (IllegalStateException ignored) {
            return "/uploads/files/" + storedFile.filename();
        }
    }

    private static void requireContentRevision(WebsiteVersion version, long expectedRevision) {
        if (version.getContentRevision() != expectedRevision) {
            throw new PreconditionFailedException(
                    "Version modifiée depuis votre dernière lecture (attendu=" + expectedRevision
                            + ", courant=" + version.getContentRevision() + ")."
            );
        }
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

    private void requireUniqueProjectSlug(Long versionId, Long projectId, ProjectRequestDTO projectRequestDTO) {
        String slug = ProjectMapper.normalizedSlug(projectRequestDTO);
        if (slug.isBlank()) {
            throw new IllegalArgumentException("Le slug du projet ne peut pas être vide.");
        }
        boolean exists = rpProject.findByWebsiteVersion_IdOrderByDisplayOrderAscStartDateDesc(versionId).stream()
                .filter(project -> projectId == null || !projectId.equals(project.getId()))
                .map(ProjectMapper::effectiveSlug)
                .anyMatch(existingSlug -> slug.equalsIgnoreCase(existingSlug));
        if (exists) {
            throw new IllegalArgumentException("Ce slug de projet est déjà utilisé dans cette version.");
        }
    }

    private Project findProjectByVersion(Long versionId, Long projectId) {
        return rpProject.findByIdAndWebsiteVersion_Id(projectId, versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
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
