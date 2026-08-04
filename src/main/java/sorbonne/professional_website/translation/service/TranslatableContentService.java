package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.repository.ExperienceRepository;
import sorbonne.professional_website.repository.ProfileRepository;
import sorbonne.professional_website.repository.ProjectRepository;
import sorbonne.professional_website.repository.TimelineRepository;
import sorbonne.professional_website.translation.entity.TranslationContentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslatableContentService {

    private final ProfileRepository profileRepository;
    private final TimelineRepository timelineRepository;
    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final ProvenSkillCatalog provenSkillCatalog;

    public TranslatableContentService(
            ProfileRepository profileRepository,
            TimelineRepository timelineRepository,
            ExperienceRepository experienceRepository,
            ProjectRepository projectRepository,
            ProvenSkillCatalog provenSkillCatalog
    ) {
        this.profileRepository = profileRepository;
        this.timelineRepository = timelineRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.provenSkillCatalog = provenSkillCatalog;
    }

    public TranslatableContent get(TranslationContentType type, String key) {
        Long numericId = type == TranslationContentType.PROVEN_SKILL ? null : parseId(key);
        return switch (type) {
            case PROFILE -> profileContent(numericId, profileRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Profile")));
            case TIMELINE -> timelineContent(numericId, timelineRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Timeline")));
            case EXPERIENCE -> experienceContent(numericId, experienceRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Experience")));
            case PROJECT -> projectContent(numericId, projectRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project")));
            case PROVEN_SKILL -> provenSkillCatalog.find(key)
                    .map(definition -> new TranslatableContent(
                            TranslationContentType.PROVEN_SKILL,
                            definition.id(),
                            definition.label(),
                            definition.translatableFields()
                    ))
                    .orElseThrow(() -> new ResourceNotFoundException("ProvenSkill"));
        };
    }

    public List<TranslatableContent> catalog() {
        List<TranslatableContent> result = new ArrayList<>();
        profileRepository.findAll().forEach(profile -> result.add(profileContent(profile.getId(), profile)));
        timelineRepository.findAll().forEach(timeline -> result.add(timelineContent(timeline.getId(), timeline)));
        experienceRepository.findAll().forEach(experience -> result.add(experienceContent(experience.getId(), experience)));
        projectRepository.findAll().forEach(project -> result.add(projectContent(project.getId(), project)));
        provenSkillCatalog.definitions().forEach(definition -> result.add(new TranslatableContent(
                TranslationContentType.PROVEN_SKILL,
                definition.id(),
                definition.label(),
                definition.translatableFields()
        )));
        return result;
    }

    private TranslatableContent profileContent(Long id, Profile profile) {
        Map<String, String> fields = linkedFields();
        put(fields, "title", profile.getTitle());
        put(fields, "subtitle", profile.getSubtitle());
        put(fields, "headline", profile.getHeadline());
        put(fields, "shortDescription", profile.getShortDescription());
        put(fields, "description", profile.getDescription());
        put(fields, "location", profile.getLocation());
        put(fields, "availability", profile.getAvailability());
        return new TranslatableContent(TranslationContentType.PROFILE, String.valueOf(id), label(profile.getTitle(), "Profil " + id), fields);
    }

    private TranslatableContent timelineContent(Long id, Timeline timeline) {
        Map<String, String> fields = linkedFields();
        put(fields, "title", timeline.getTitle());
        put(fields, "description", timeline.getDescription());
        return new TranslatableContent(TranslationContentType.TIMELINE, String.valueOf(id), label(timeline.getTitle(), "Timeline " + id), fields);
    }

    private TranslatableContent experienceContent(Long id, Experience experience) {
        Map<String, String> fields = linkedFields();
        put(fields, "title", experience.getTitle());
        put(fields, "location", experience.getLocation());
        put(fields, "summary", experience.getSummary());
        put(fields, "description", experience.getDescription());
        return new TranslatableContent(TranslationContentType.EXPERIENCE, String.valueOf(id), label(experience.getTitle(), "Expérience " + id), fields);
    }

    private TranslatableContent projectContent(Long id, Project project) {
        Map<String, String> fields = linkedFields();
        put(fields, "title", project.getTitle());
        put(fields, "subtitle", project.getSubtitle());
        put(fields, "shortDescription", project.getShortDescription());
        put(fields, "description", project.getDescription());
        if (project.getFeatures() != null && !project.getFeatures().isEmpty()) {
            put(fields, "features", String.join("\n", project.getFeatures()));
        }
        return new TranslatableContent(TranslationContentType.PROJECT, String.valueOf(id), label(project.getTitle(), "Projet " + id), fields);
    }

    private static String label(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, String> linkedFields() {
        return new LinkedHashMap<>();
    }

    private static void put(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, value);
        }
    }

    private static Long parseId(String key) {
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid numeric content key: " + key, exception);
        }
    }
}
