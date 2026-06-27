package sorbonne.professional_website.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.response.ExperienceResponseDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.ProvenSkillResponseDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ExperienceMapper;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.repository.OwnerRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class WebsiteService {

    private static final Pattern NON_LATIN_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private static final List<SkillDefinition> SKILL_DEFINITIONS = List.of(
            new SkillDefinition(
                    "backend-architecture",
                    "Backend & architecture",
                    "Backend",
                    "Backend",
                    "API REST, règles métier, persistance, versioning et services maintenables.",
                    List.of("java", "spring", "spring boot", "api", "backend", "jpa", "hibernate", "flyway", "maven", "validation", "architecture", "rest")
            ),
            new SkillDefinition(
                    "data-pipelines",
                    "Data pipelines",
                    "Data",
                    "Data",
                    "Collecte, ingestion, transformation, stockage PostgreSQL et exploitation de données volumineuses.",
                    List.of("data", "ais", "pipeline", "ingestion", "csv", "postgresql", "python", "systemd", "batch", "export", "nmea")
            ),
            new SkillDefinition(
                    "frontend-product",
                    "Interfaces produit",
                    "Frontend",
                    "Frontend",
                    "Interfaces React et web orientées usage, filtres, modales, interactions et lisibilité recruteur.",
                    List.of("react", "mantine", "tailwind", "frontend", "ui", "ux", "web", "gsap", "vite", "interface", "symfony", "twig")
            ),
            new SkillDefinition(
                    "graphics-performance",
                    "Performance graphique",
                    "Graphique",
                    "Graphique",
                    "Rendu natif, visualisation, interactions temps réel et séparation stricte UI / moteur.",
                    List.of("opengl", "jogl", "jni", "javafx", "graph", "graphe", "performance", "rendu", "native", "c")
            ),
            new SkillDefinition(
                    "devops-deployment",
                    "DevOps & déploiement",
                    "DevOps",
                    "DevOps",
                    "Dockerisation, environnements reproductibles, exposition cloud, stockage fichiers et supervision.",
                    List.of("docker", "kubernetes", "cloudflare", "render", "neon", "minio", "redis", "hpa", "ingress", "cloudinary", "systemd")
            ),
            new SkillDefinition(
                    "software-quality",
                    "Qualité logicielle",
                    "Qualité",
                    "Qualité",
                    "Structuration, tests, robustesse, documentation, maintenabilité et gestion des cas limites.",
                    List.of("test", "tests", "documentation", "mvc", "robuste", "qualité", "maintenable", "validation", "refactor", "séparation", "architecture")
            )
    );

    private final OwnerRepository rpOwner;

    public WebsiteService(OwnerRepository rpOwner) {
        this.rpOwner = rpOwner;
    }

    public List<OwnerResponseDTO> getAllPublicWebsites() {
        return rpOwner.findAll()
                .stream()
                .filter(owner -> Boolean.TRUE.equals(owner.getActive()))
                .filter(owner -> owner.getActiveWebsiteVersion().isPresent())
                .map(OwnerMapper::toResponse)
                .toList();
    }

    public OwnerResponseDTO getPublicWebsiteByOwnerId(Long ownerId) {
        Owner owner = findPublicOwner(ownerId);
        return OwnerMapper.toResponse(owner);
    }

    public OwnerResponseDTO getFirstOwner() {
        Owner owner = rpOwner.findFirstByOrderByOwnerIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("No owner found"));

        return OwnerMapper.toResponse(owner);
    }

    public ProjectResponseDTO getDefaultProjectBySlug(String projectSlug) {
        Owner owner = rpOwner.findFirstByOrderByOwnerIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("No owner found"));
        return ProjectMapper.toResponse(findPublishedProjectBySlug(owner, projectSlug));
    }

    public ProjectResponseDTO getProjectByOwnerAndSlug(Long ownerId, String projectSlug) {
        Owner owner = findPublicOwner(ownerId);
        return ProjectMapper.toResponse(findPublishedProjectBySlug(owner, projectSlug));
    }

    public List<ProvenSkillResponseDTO> getDefaultProvenSkills() {
        Owner owner = rpOwner.findFirstByOrderByOwnerIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("No owner found"));
        return buildProvenSkills(owner);
    }

    public List<ProvenSkillResponseDTO> getProvenSkillsByOwner(Long ownerId) {
        Owner owner = findPublicOwner(ownerId);
        return buildProvenSkills(owner);
    }

    private Owner findPublicOwner(Long ownerId) {
        Owner owner = rpOwner.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));

        if (!Boolean.TRUE.equals(owner.getActive())) {
            throw new ResourceNotFoundException("Website");
        }

        if (owner.getActiveWebsiteVersion().isEmpty()) {
            throw new ResourceNotFoundException("Active WebsiteVersion");
        }

        return owner;
    }

    private Project findPublishedProjectBySlug(Owner owner, String projectSlug) {
        WebsiteVersion version = owner.getActiveWebsiteVersion()
                .orElseThrow(() -> new ResourceNotFoundException("Active WebsiteVersion"));
        String normalizedSlug = slugify(projectSlug);

        return safeProjects(version).stream()
                .filter(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished()))
                .filter(project -> normalizedSlug.equals(slugify(defaultIfBlank(project.getSlug(), project.getTitle()))))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
    }

    private List<ProvenSkillResponseDTO> buildProvenSkills(Owner owner) {
        WebsiteVersion version = owner.getActiveWebsiteVersion()
                .orElseThrow(() -> new ResourceNotFoundException("Active WebsiteVersion"));
        List<Project> projects = safeProjects(version).stream()
                .filter(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished()))
                .toList();
        List<Experience> experiences = safeExperiences(version.getTimeline());

        return SKILL_DEFINITIONS.stream()
                .map(definition -> buildProvenSkill(definition, projects, experiences))
                .filter(skill -> skill.evidenceCount() != null && skill.evidenceCount() > 0)
                .sorted(Comparator.comparing(ProvenSkillResponseDTO::score, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProvenSkillResponseDTO::label))
                .limit(6)
                .toList();
    }

    private ProvenSkillResponseDTO buildProvenSkill(SkillDefinition definition, List<Project> projects, List<Experience> experiences) {
        List<ProjectMatch> projectMatches = projects.stream()
                .map(project -> new ProjectMatch(project, matchScore(projectSearchText(project), definition.terms()) + proofTagScore(project, definition.terms())))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparing(ProjectMatch::score, Comparator.reverseOrder())
                        .thenComparing(match -> match.project().getDisplayOrder() == null ? 999 : match.project().getDisplayOrder()))
                .toList();

        List<ExperienceMatch> experienceMatches = experiences.stream()
                .map(experience -> new ExperienceMatch(experience, matchScore(experienceSearchText(experience), definition.terms())))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparing(ExperienceMatch::score, Comparator.reverseOrder())
                        .thenComparing(match -> match.experience().getDisplayOrder() == null ? 999 : match.experience().getDisplayOrder()))
                .toList();

        int score = projectMatches.stream().mapToInt(ProjectMatch::score).sum()
                + experienceMatches.stream().mapToInt(ExperienceMatch::score).sum();
        int evidenceCount = projectMatches.size() + experienceMatches.size();

        List<Project> evidenceProjects = projectMatches.stream().map(ProjectMatch::project).limit(4).toList();
        List<Experience> evidenceExperiences = experienceMatches.stream().map(ExperienceMatch::experience).limit(3).toList();
        List<String> stacks = uniqueStrings(evidenceProjects.stream().flatMap(project -> safeStrings(project.getStacks()).stream()).toList()).stream().limit(8).toList();
        List<String> proofPoints = buildProofPoints(evidenceProjects, evidenceExperiences).stream().limit(6).toList();
        List<String> projectSlugs = evidenceProjects.stream().map(project -> defaultIfBlank(project.getSlug(), slugify(project.getTitle()))).toList();
        List<String> experienceTitles = evidenceExperiences.stream().map(Experience::getTitle).toList();

        return new ProvenSkillResponseDTO(
                definition.id(),
                definition.label(),
                definition.shortLabel(),
                definition.category(),
                definition.description(),
                definition.description(),
                Math.min(5, Math.max(1, score / 3)),
                score,
                evidenceCount,
                stacks,
                proofPoints,
                evidenceProjects.stream().map(ProjectMapper::toResponse).toList(),
                evidenceExperiences.stream().map(ExperienceMapper::toResponse).toList(),
                projectSlugs,
                experienceTitles
        );
    }

    private List<String> buildProofPoints(List<Project> projects, List<Experience> experiences) {
        List<String> points = new ArrayList<>();
        for (Project project : projects) {
            points.addAll(safeStrings(project.getProofTags()));
            points.addAll(safeStrings(project.getFeatures()).stream().limit(2).toList());
        }
        for (Experience experience : experiences) {
            points.addAll(safeStrings(experience.getSkills()).stream().limit(3).toList());
        }
        return uniqueStrings(points);
    }

    private int proofTagScore(Project project, List<String> terms) {
        String text = String.join(" ", safeStrings(project.getProofTags()));
        return matchScore(text, terms) * 2;
    }

    private int matchScore(String text, List<String> terms) {
        String normalized = normalize(text);
        int score = 0;
        for (String term : terms) {
            if (normalized.contains(normalize(term))) {
                score++;
            }
        }
        return score;
    }

    private String projectSearchText(Project project) {
        return String.join(" ", safeStrings(Arrays.asList(
                project.getTitle(),
                project.getSubtitle(),
                project.getShortDescription(),
                project.getDescription(),
                project.getCaseStudyProblem(),
                project.getCaseStudyContext(),
                project.getCaseStudyRole(),
                project.getCaseStudyArchitecture(),
                project.getCaseStudyNextSteps()
        )))
                + " " + String.join(" ", safeStrings(project.getStacks()))
                + " " + String.join(" ", safeStrings(project.getFeatures()))
                + " " + String.join(" ", safeStrings(project.getProofTags()))
                + " " + String.join(" ", safeStrings(project.getCaseStudyTechnicalChoices()))
                + " " + String.join(" ", safeStrings(project.getCaseStudyChallenges()))
                + " " + String.join(" ", safeStrings(project.getCaseStudyOutcomes()))
                + " " + String.join(" ", safeStrings(project.getCaseStudyResults()));
    }

    private String experienceSearchText(Experience experience) {
        return String.join(" ", safeStrings(Arrays.asList(
                experience.getTitle(),
                experience.getOrganization(),
                experience.getSummary(),
                experience.getDescription()
        ))) + " " + String.join(" ", safeStrings(experience.getSkills()));
    }

    private List<Project> safeProjects(WebsiteVersion version) {
        if (version == null || version.getProjects() == null) {
            return List.of();
        }
        return version.getProjects();
    }

    private List<Experience> safeExperiences(Timeline timeline) {
        if (timeline == null || timeline.getExperiences() == null) {
            return List.of();
        }
        return timeline.getExperiences();
    }

    private List<String> safeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private List<String> uniqueStrings(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        Map<String, String> canonicalValues = new LinkedHashMap<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            String key = normalize(value);
            if (seen.add(key)) {
                canonicalValues.put(key, value);
            }
        }
        return new ArrayList<>(canonicalValues.values());
    }

    private String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "projet";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = NON_LATIN_MARKS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_SLUG_CHARS.matcher(normalized).replaceAll("-");
        normalized = EDGE_DASHES.matcher(normalized).replaceAll("");
        return normalized.isBlank() ? "projet" : normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = NON_LATIN_MARKS.matcher(normalized).replaceAll("");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record SkillDefinition(String id, String label, String shortLabel, String category, String description, List<String> terms) {
    }

    private record ProjectMatch(Project project, Integer score) {
    }

    private record ExperienceMatch(Experience experience, Integer score) {
    }
}
