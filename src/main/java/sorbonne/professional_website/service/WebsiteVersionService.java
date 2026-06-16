package sorbonne.professional_website.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.PortfolioRestoreRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioBackupResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioHealthCheckResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioHealthReportResponseDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.ContactInfo;
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
import sorbonne.professional_website.upload.StorageService;
import sorbonne.professional_website.upload.StoredFile;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.ProjectRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@Transactional
public class WebsiteVersionService {

    private final OwnerRepository rpOwner;
    private final WebsiteVersionRepository rpWebsiteVersion;
    private final ProjectRepository rpProject;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public WebsiteVersionService(
            OwnerRepository rpOwner,
            WebsiteVersionRepository rpWebsiteVersion,
            ProjectRepository rpProject,
            StorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.rpOwner = rpOwner;
        this.rpWebsiteVersion = rpWebsiteVersion;
        this.rpProject = rpProject;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
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


    @Transactional(readOnly = true)
    public PortfolioHealthReportResponseDTO getHealthReport(Long ownerId, Long versionId) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        return buildHealthReport(ownerId, versionId, version);
    }

    @Transactional(readOnly = true)
    public PortfolioHealthReportResponseDTO validateBeforePublish(Long ownerId, Long versionId) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        return buildHealthReport(ownerId, versionId, version);
    }

    public WebsiteVersionResponseDTO activateVersionAfterValidation(Long ownerId, Long versionId) {
        lockOwner(ownerId);
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        PortfolioHealthReportResponseDTO report = buildHealthReport(ownerId, versionId, version);
        if (!report.publishable()) {
            throw new IllegalStateException("Publication bloquée : corrige les erreurs critiques avant activation.");
        }
        return activateVersion(ownerId, versionId);
    }

    @Transactional(readOnly = true)
    public PortfolioBackupResponseDTO exportVersionBackup(Long ownerId, Long versionId) {
        WebsiteVersion version = findVersionByOwner(ownerId, versionId);
        String filename = buildBackupFilename(version);
        String json = buildBackupJson(ownerId, version);
        byte[] zipBytes = buildBackupZip(json, version);
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
            restoredRequest = readVersionRequestFromBackupJson(requestDTO.backupJson());
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

    private PortfolioHealthReportResponseDTO buildHealthReport(Long ownerId, Long versionId, WebsiteVersion version) {
        List<PortfolioHealthCheckResponseDTO> checks = new ArrayList<>();

        Profile profile = version.getProfile();
        Timeline timeline = version.getTimeline();
        List<Project> versionProjects = version.getProjects() == null ? List.of() : version.getProjects();
        List<Experience> versionExperiences = timeline == null || timeline.getExperiences() == null ? List.of() : timeline.getExperiences();
        Owner owner = version.getOwner();

        addCheck(checks, "profile.title", "Titre du profil", "BLOCKER", !isBlank(profile == null ? null : profile.getTitle()), "Le profil doit avoir un titre public.");
        addCheck(checks, "profile.description", "Description du profil", "BLOCKER", !isBlank(profile == null ? null : profile.getDescription()), "La description publique du profil est obligatoire.");
        addCheck(checks, "profile.cv", "CV attaché", "WARNING", profile != null && !isBlank(profile.getCvUrl()), "Aucun CV n'est attaché à cette version.");
        addCheck(checks, "timeline", "Timeline", "BLOCKER", timeline != null && !versionExperiences.isEmpty(), "La timeline doit contenir au moins une expérience ou formation.");
        addCheck(checks, "projects.published", "Projets publiés", "BLOCKER", versionProjects.stream().anyMatch(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished())), "Au moins un projet publié est nécessaire.");
        addCheck(checks, "projects.featured", "Projet mis en avant", "WARNING", versionProjects.stream().anyMatch(project -> Boolean.TRUE.equals(project.getFeatured())), "Aucun projet n'est marqué comme featured.");
        addCheck(checks, "assets.profile", "Image profil", "SUGGESTION", profile != null && !isBlank(profile.getProfileImageUrl()), "Ajoute une image de profil pour un rendu public plus professionnel.");
        addCheck(checks, "contacts.email", "Contact email", "BLOCKER", owner != null && hasContact(owner, "EMAIL"), "Ajoute un email dans les contacts owner.");
        addCheck(checks, "contacts.github", "Lien GitHub", "WARNING", owner != null && hasContact(owner, "GITHUB"), "Ajoute un lien GitHub dans les contacts owner.");
        addCheck(checks, "links.projects", "Liens projets", "SUGGESTION", versionProjects.stream().anyMatch(project -> !isBlank(project.getGithubUrl()) || !isBlank(project.getDocumentationUrl())), "Ajoute au moins un lien GitHub ou documentation sur les projets publiés.");
        addCheck(checks, "version.published", "Version publiée", "WARNING", Boolean.TRUE.equals(version.getPublished()), "La version n'est pas marquée comme published.");
        addCheck(checks, "version.active", "Version active", "SUGGESTION", Boolean.TRUE.equals(version.getActive()), "La version n'est pas active. Utilise la validation avant publication.");
        addCheck(checks, "dates.experiences", "Dates expériences", "WARNING", versionExperiences.stream().allMatch(experience -> experience.getStartDate() != null), "Certaines expériences n'ont pas de date de début.");
        addCheck(checks, "projects.images", "Images projets", "SUGGESTION", versionProjects.stream().filter(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished())).allMatch(project -> !isBlank(project.getImageUrl())), "Certains projets publiés n'ont pas d'image.");

        long blockers = checks.stream().filter(check -> "BLOCKER".equals(check.severity()) && "FAIL".equals(check.status())).count();
        long warnings = checks.stream().filter(check -> "WARNING".equals(check.severity()) && "FAIL".equals(check.status())).count();
        long suggestions = checks.stream().filter(check -> "SUGGESTION".equals(check.severity()) && "FAIL".equals(check.status())).count();
        int score = Math.max(0, 100 - (int) blockers * 30 - (int) warnings * 8 - (int) suggestions * 3);

        return new PortfolioHealthReportResponseDTO(
                score,
                blockers == 0,
                (int) blockers,
                (int) warnings,
                (int) suggestions,
                checks,
                LocalDateTime.now(),
                ownerId,
                versionId
        );
    }

    private void addCheck(List<PortfolioHealthCheckResponseDTO> checks, String id, String label, String severity, boolean pass, String failureMessage) {
        checks.add(new PortfolioHealthCheckResponseDTO(
                id,
                label,
                severity,
                pass ? "PASS" : "FAIL",
                pass ? "OK" : failureMessage
        ));
    }

    private boolean hasContact(Owner owner, String type) {
        if (owner.getContacts() == null) {
            return false;
        }
        return owner.getContacts().stream()
                .filter(Objects::nonNull)
                .anyMatch(contact -> contact.getType() != null && type.equals(contact.getType().name()) && !isBlank(contact.getValue()));
    }

    private String buildBackupJson(Long ownerId, WebsiteVersion version) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            Owner owner = version.getOwner();
            WebsiteVersionRequestDTO versionRequest = toVersionRequest(version);
            payload.put("format", "portfolio-backup-v1");
            payload.put("generatedAt", LocalDateTime.now().toString());
            payload.put("ownerId", ownerId);
            payload.put("sourceVersionId", version.getId());
            payload.put("owner", toOwnerBackupMap(owner));
            payload.put("versionRequest", versionRequest);
            payload.put("exportedVersion", WebsiteVersionMapper.toResponse(version));
            payload.put("health", buildHealthReport(ownerId, version.getId(), version));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de sérialiser le backup portfolio.", exception);
        }
    }

    private Map<String, Object> toOwnerBackupMap(Owner owner) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (owner == null) {
            return map;
        }
        map.put("name", owner.getName());
        map.put("firstName", owner.getFirstName());
        map.put("age", owner.getAge());
        map.put("active", owner.getActive());
        map.put("address", owner.getAddress());
        map.put("contacts", owner.getContacts() == null ? List.of() : owner.getContacts().stream().map(this::toContactMap).toList());
        return map;
    }

    private Map<String, Object> toContactMap(ContactInfo contact) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", contact.getType() == null ? null : contact.getType().name());
        map.put("value", contact.getValue());
        return map;
    }

    private WebsiteVersionRequestDTO toVersionRequest(WebsiteVersion version) {
        return new WebsiteVersionRequestDTO(
                version.getVersionTag(),
                version.getLabel(),
                version.getDescription(),
                false,
                version.getPublished(),
                toProfileRequest(version.getProfile()),
                toTimelineRequest(version.getTimeline()),
                version.getProjects() == null ? List.of() : version.getProjects().stream().map(this::toProjectRequest).toList()
        );
    }

    private ProfileRequestDTO toProfileRequest(Profile profile) {
        if (profile == null) {
            return null;
        }
        return new ProfileRequestDTO(
                profile.getTitle(),
                profile.getSubtitle(),
                profile.getHeadline(),
                profile.getShortDescription(),
                profile.getDescription(),
                profile.getLocation(),
                profile.getAvailability(),
                profile.getProfileImageUrl(),
                profile.getLogoUrl(),
                profile.getCvUrl(),
                profile.getPortfolioUrl()
        );
    }

    private TimelineRequestDTO toTimelineRequest(Timeline timeline) {
        if (timeline == null) {
            return null;
        }
        return new TimelineRequestDTO(
                timeline.getTitle(),
                timeline.getDescription(),
                timeline.getExperiences() == null ? List.of() : timeline.getExperiences().stream().map(this::toExperienceRequest).toList()
        );
    }

    private sorbonne.professional_website.dto.request.ExperienceRequestDTO toExperienceRequest(Experience experience) {
        return new sorbonne.professional_website.dto.request.ExperienceRequestDTO(
                experience.getCategory(),
                experience.getTitle(),
                experience.getOrganization(),
                experience.getLocation(),
                experience.getSummary(),
                experience.getDescription(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.isCurrentPosition(),
                experience.getImageUrl(),
                experience.getWebsiteUrl(),
                experience.getSkills(),
                experience.getDisplayOrder()
        );
    }

    private ProjectRequestDTO toProjectRequest(Project project) {
        return new ProjectRequestDTO(
                project.getTitle(),
                project.getSubtitle(),
                project.getShortDescription(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getImageUrl(),
                project.getDemoUrl(),
                project.getGithubUrl(),
                project.getDocumentationUrl(),
                project.getStacks(),
                project.getFeatures(),
                project.getLinks() == null ? List.of() : project.getLinks().stream().map(link -> new sorbonne.professional_website.dto.request.ProjectLinkRequestDTO(link.getType(), link.getLabel(), link.getUrl())).toList(),
                project.getFeatured(),
                project.getPublished(),
                project.getDisplayOrder(),
                null
        );
    }

    private WebsiteVersionRequestDTO readVersionRequestFromBackupJson(String backupJson) {
        try {
            JsonNode root = objectMapper.readTree(backupJson);
            JsonNode versionRequestNode = root.get("versionRequest");
            if (versionRequestNode != null && !versionRequestNode.isNull()) {
                return objectMapper.treeToValue(versionRequestNode, WebsiteVersionRequestDTO.class);
            }
            return objectMapper.treeToValue(root, WebsiteVersionRequestDTO.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Backup JSON illisible ou incompatible : " + exception.getMessage(), exception);
        }
    }

    private byte[] buildBackupZip(String json, WebsiteVersion version) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            writeZipEntry(zip, "portfolio.json", json.getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "metadata.json", ("{\n"
                    + "  \"format\": \"portfolio-backup-v1\",\n"
                    + "  \"versionId\": " + version.getId() + ",\n"
                    + "  \"versionTag\": \"" + escapeJson(version.getVersionTag()) + "\",\n"
                    + "  \"label\": \"" + escapeJson(version.getLabel()) + "\"\n"
                    + "}\n").getBytes(StandardCharsets.UTF_8));
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de générer le ZIP de backup.", exception);
        }
    }

    private void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes == null ? new byte[0] : bytes);
        zip.closeEntry();
    }

    private String buildBackupFilename(WebsiteVersion version) {
        String tag = defaultIfBlank(version.getVersionTag(), "version").toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "portfolio-backup-" + defaultIfBlank(tag, "version") + ".zip";
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

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
