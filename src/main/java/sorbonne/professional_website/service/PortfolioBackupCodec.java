package sorbonne.professional_website.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import sorbonne.professional_website.dto.request.ExperienceRequestDTO;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectCaseStudyRequestDTO;
import sorbonne.professional_website.dto.request.ProjectLinkRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.entity.ContactInfo;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class PortfolioBackupCodec {

    private final ObjectMapper objectMapper;
    private final PortfolioHealthEvaluator healthEvaluator;

    public PortfolioBackupCodec(ObjectMapper objectMapper, PortfolioHealthEvaluator healthEvaluator) {
        this.objectMapper = objectMapper;
        this.healthEvaluator = healthEvaluator;
    }

    public BackupArtifact encode(Long ownerId, WebsiteVersion version) {
        String json = buildBackupJson(ownerId, version);
        return new BackupArtifact(buildBackupFilename(version), json, buildBackupZip(json, version));
    }

    public WebsiteVersionRequestDTO decodeVersionRequest(String backupJson) {
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

    private String buildBackupJson(Long ownerId, WebsiteVersion version) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            Owner owner = version.getOwner();
            payload.put("format", "portfolio-backup-v1");
            payload.put("generatedAt", LocalDateTime.now().toString());
            payload.put("ownerId", ownerId);
            payload.put("sourceVersionId", version.getId());
            payload.put("owner", toOwnerBackupMap(owner));
            payload.put("versionRequest", toVersionRequest(version));
            payload.put("exportedVersion", WebsiteVersionMapper.toResponse(version));
            payload.put("health", healthEvaluator.evaluate(ownerId, version.getId(), version));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de sérialiser le backup portfolio.", exception);
        }
    }

    private Map<String, Object> toOwnerBackupMap(Owner owner) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (owner == null) return map;
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
                version.getVersionTag(), version.getLabel(), version.getDescription(), false, version.getPublished(),
                toProfileRequest(version.getProfile()), toTimelineRequest(version.getTimeline()),
                version.getProjects() == null ? List.of() : version.getProjects().stream().map(this::toProjectRequest).toList()
        );
    }

    private ProfileRequestDTO toProfileRequest(Profile profile) {
        if (profile == null) return null;
        return new ProfileRequestDTO(
                profile.getTitle(), profile.getSubtitle(), profile.getHeadline(), profile.getShortDescription(),
                profile.getDescription(), profile.getLocation(), profile.getAvailability(), profile.getProfileImageUrl(),
                profile.getLogoUrl(), profile.getCvUrl(), profile.getPortfolioUrl()
        );
    }

    private TimelineRequestDTO toTimelineRequest(Timeline timeline) {
        if (timeline == null) return null;
        return new TimelineRequestDTO(
                timeline.getTitle(), timeline.getDescription(),
                timeline.getExperiences() == null ? List.of() : timeline.getExperiences().stream().map(this::toExperienceRequest).toList()
        );
    }

    private ExperienceRequestDTO toExperienceRequest(Experience experience) {
        return new ExperienceRequestDTO(
                experience.getCategory(), experience.getTitle(), experience.getOrganization(), experience.getLocation(),
                experience.getSummary(), experience.getDescription(), experience.getStartDate(), experience.getEndDate(),
                experience.isCurrentPosition(), experience.getImageUrl(), experience.getWebsiteUrl(), experience.getSkills(),
                experience.getDisplayOrder()
        );
    }

    private ProjectRequestDTO toProjectRequest(Project project) {
        return new ProjectRequestDTO(
                project.getTitle(), project.getSubtitle(), project.getShortDescription(), project.getDescription(), project.getStatus(),
                project.getStartDate(), project.getEndDate(), project.getImageUrl(), project.getDemoUrl(), project.getGithubUrl(),
                project.getDocumentationUrl(), project.getArchitectureUrl(), project.getSlug(), project.getStacks(), project.getFeatures(),
                project.getProofTags(), toCaseStudyRequest(project),
                project.getLinks() == null ? List.of() : project.getLinks().stream()
                        .map(link -> new ProjectLinkRequestDTO(link.getType(), link.getLabel(), link.getUrl())).toList(),
                project.getFeatured(), project.getPublished(), project.getDisplayOrder(), null
        );
    }

    private ProjectCaseStudyRequestDTO toCaseStudyRequest(Project project) {
        return new ProjectCaseStudyRequestDTO(
                project.getCaseStudyProblem(), project.getCaseStudyContext(), project.getCaseStudyRole(),
                project.getCaseStudyArchitecture(), project.getCaseStudyTechnicalChoices(), project.getCaseStudyChallenges(),
                project.getCaseStudySolutions(), project.getCaseStudyOutcomes(), project.getCaseStudyResults(),
                project.getCaseStudyLimits(), project.getCaseStudyNextSteps()
        );
    }

    private byte[] buildBackupZip(String json, WebsiteVersion version) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
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

    private static void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes == null ? new byte[0] : bytes);
        zip.closeEntry();
    }

    private static String buildBackupFilename(WebsiteVersion version) {
        String tag = defaultIfBlank(version.getVersionTag(), "version")
                .toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "portfolio-backup-" + defaultIfBlank(tag, "version") + ".zip";
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record BackupArtifact(String filename, String json, byte[] zipBytes) { }
}
