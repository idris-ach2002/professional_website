package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.dto.response.ExperienceResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.ProvenSkillResponseDTO;
import sorbonne.professional_website.translation.entity.TranslationContentType;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProvenSkillService {

    private final ProvenSkillCatalog catalog;
    private final TranslationStoreService translationStoreService;

    public ProvenSkillService(ProvenSkillCatalog catalog, TranslationStoreService translationStoreService) {
        this.catalog = catalog;
        this.translationStoreService = translationStoreService;
    }

    public List<ProvenSkillResponseDTO> build(
            List<ProjectResponseDTO> sourceProjects,
            List<ExperienceResponseDTO> sourceExperiences,
            List<ProjectResponseDTO> localizedProjects,
            List<ExperienceResponseDTO> localizedExperiences,
            String locale
    ) {
        Map<Long, ProjectResponseDTO> localizedProjectById = localizedProjects.stream()
                .filter(item -> item.id() != null)
                .collect(Collectors.toMap(ProjectResponseDTO::id, Function.identity(), (left, right) -> right));
        Map<Long, ExperienceResponseDTO> localizedExperienceById = localizedExperiences.stream()
                .filter(item -> item.id() != null)
                .collect(Collectors.toMap(ExperienceResponseDTO::id, Function.identity(), (left, right) -> right));

        return catalog.definitions().stream()
                .map(definition -> buildSkill(
                        definition,
                        sourceProjects,
                        sourceExperiences,
                        localizedProjectById,
                        localizedExperienceById,
                        locale
                ))
                .filter(skill -> skill.evidenceCount() != null && skill.evidenceCount() > 0)
                .sorted(Comparator
                        .comparing(ProvenSkillResponseDTO::score, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProvenSkillResponseDTO::label, String.CASE_INSENSITIVE_ORDER))
                .limit(6)
                .toList();
    }

    private ProvenSkillResponseDTO buildSkill(
            ProvenSkillCatalog.Definition definition,
            List<ProjectResponseDTO> sourceProjects,
            List<ExperienceResponseDTO> sourceExperiences,
            Map<Long, ProjectResponseDTO> localizedProjectById,
            Map<Long, ExperienceResponseDTO> localizedExperienceById,
            String locale
    ) {
        List<ScoredProject> projects = sourceProjects.stream()
                .filter(project -> !Boolean.FALSE.equals(project.published()))
                .map(project -> scoreProject(project, definition))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredProject::score).reversed()
                        .thenComparing(item -> Optional.ofNullable(item.project().displayOrder()).orElse(999)))
                .toList();

        List<ScoredExperience> experiences = sourceExperiences.stream()
                .map(experience -> scoreExperience(experience, definition))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredExperience::score).reversed()
                        .thenComparing(item -> Optional.ofNullable(item.experience().displayOrder()).orElse(999)))
                .toList();

        Map<String, String> translated = translationStoreService.publishedFields(
                TranslationContentType.PROVEN_SKILL,
                definition.id(),
                locale
        );

        List<ProjectResponseDTO> relatedProjects = projects.stream()
                .map(item -> localizedProjectById.getOrDefault(item.project().id(), item.project()))
                .limit(4)
                .toList();
        List<ExperienceResponseDTO> relatedExperiences = experiences.stream()
                .map(item -> localizedExperienceById.getOrDefault(item.experience().id(), item.experience()))
                .limit(3)
                .toList();

        List<String> stacks = unique(projects.stream()
                .flatMap(item -> safe(item.project().stacks()).stream())
                .toList()).stream().limit(8).toList();
        List<String> proofPoints = unique(projects.stream()
                .flatMap(item -> item.matches().stream())
                .toList()).stream().limit(8).toList();
        int score = projects.stream().mapToInt(ScoredProject::score).sum()
                + experiences.stream().mapToInt(ScoredExperience::score).sum();
        int evidenceCount = projects.size() + experiences.size();

        return new ProvenSkillResponseDTO(
                definition.id(),
                translated.getOrDefault("label", definition.label()),
                translated.getOrDefault("shortLabel", definition.shortLabel()),
                translated.getOrDefault("category", definition.category()),
                translated.getOrDefault("description", definition.description()),
                translated.getOrDefault("description", definition.description()),
                Math.min(100, 50 + score * 4),
                score,
                evidenceCount,
                stacks,
                proofPoints,
                relatedProjects,
                relatedExperiences,
                relatedProjects.stream().map(ProjectResponseDTO::slug).toList(),
                relatedExperiences.stream().map(ExperienceResponseDTO::title).toList()
        );
    }

    private ScoredProject scoreProject(ProjectResponseDTO project, ProvenSkillCatalog.Definition definition) {
        String text = normalize(String.join(" ",
                value(project.title()),
                value(project.subtitle()),
                value(project.shortDescription()),
                value(project.description()),
                String.join(" ", safe(project.stacks())),
                String.join(" ", safe(project.features()))
        ));
        List<String> matches = definition.terms().stream()
                .filter(term -> text.contains(normalize(term)))
                .toList();
        int score = matches.size() + (Boolean.TRUE.equals(project.featured()) && !matches.isEmpty() ? 1 : 0);
        return new ScoredProject(project, matches, score);
    }

    private ScoredExperience scoreExperience(ExperienceResponseDTO experience, ProvenSkillCatalog.Definition definition) {
        String text = normalize(String.join(" ",
                value(experience.title()),
                value(experience.organization()),
                value(experience.summary()),
                value(experience.description()),
                String.join(" ", safe(experience.skills()))
        ));
        List<String> matches = definition.terms().stream()
                .filter(term -> text.contains(normalize(term)))
                .toList();
        return new ScoredExperience(experience, matches, matches.size());
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static List<String> unique(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).forEach(seen::add);
        return new ArrayList<>(seen);
    }

    private record ScoredProject(ProjectResponseDTO project, List<String> matches, int score) {
    }

    private record ScoredExperience(ExperienceResponseDTO experience, List<String> matches, int score) {
    }
}
