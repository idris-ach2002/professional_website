package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.dto.response.*;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.translation.entity.TranslationContentType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioLocalizationService {

    private final TranslationStoreService translations;
    private final LocaleNormalizer localeNormalizer;
    private final ProvenSkillService provenSkillService;

    public PortfolioLocalizationService(
            TranslationStoreService translations,
            LocaleNormalizer localeNormalizer,
            ProvenSkillService provenSkillService
    ) {
        this.translations = translations;
        this.localeNormalizer = localeNormalizer;
        this.provenSkillService = provenSkillService;
    }

    public OwnerResponseDTO localize(Owner owner, String requestedLocale) {
        return localizeSource(OwnerMapper.toResponse(owner), requestedLocale);
    }

    public OwnerResponseDTO localizePublic(Owner owner, String requestedLocale) {
        return localizeSource(OwnerMapper.toPublicResponse(owner), requestedLocale);
    }

    private OwnerResponseDTO localizeSource(OwnerResponseDTO source, String requestedLocale) {
        String locale = localeNormalizer.normalize(requestedLocale);

        TranslationStoreService.PublishedTranslations publishedTranslations = translations.publishedTranslations(locale);
        ProfileResponseDTO profile = localizeProfile(source.prof(), publishedTranslations);
        TimelineResponseDTO timeline = localizeTimeline(source.timeline(), publishedTranslations);
        List<ProjectResponseDTO> projects = source.projects().stream()
                .map(project -> localizeProject(project, publishedTranslations))
                .toList();

        List<ExperienceResponseDTO> sourceExperiences = source.timeline() == null
                ? List.of()
                : source.timeline().experiences();
        List<ExperienceResponseDTO> localizedExperiences = timeline == null
                ? List.of()
                : timeline.experiences();
        List<ProvenSkillResponseDTO> provenSkills = provenSkillService.build(
                source.projects(),
                sourceExperiences,
                projects,
                localizedExperiences,
                locale,
                publishedTranslations
        );

        return new OwnerResponseDTO(
                source.ownerId(),
                source.rowVersion(),
                source.name(),
                source.firstName(),
                source.age(),
                source.active(),
                source.address(),
                source.contacts(),
                profile,
                timeline,
                projects,
                source.websiteVersions(),
                locale,
                provenSkills
        );
    }

    public ProjectResponseDTO localizeProject(ProjectResponseDTO project, String requestedLocale) {
        if (project == null) return null;
        String locale = localeNormalizer.normalize(requestedLocale);
        return localizeProject(project, translations.publishedTranslations(locale));
    }

    private ProjectResponseDTO localizeProject(
            ProjectResponseDTO project,
            TranslationStoreService.PublishedTranslations publishedTranslations
    ) {
        Map<String, String> fields = translations.publishedFields(
                publishedTranslations,
                TranslationContentType.PROJECT,
                String.valueOf(project.id()),
                projectSourceFields(project)
        );
        return new ProjectResponseDTO(
                project.id(),
                fields.getOrDefault("title", project.title()),
                fields.getOrDefault("subtitle", project.subtitle()),
                fields.getOrDefault("shortDescription", project.shortDescription()),
                fields.getOrDefault("description", project.description()),
                project.status(),
                project.startDate(),
                project.endDate(),
                project.imageUrl(),
                project.demoUrl(),
                project.githubUrl(),
                project.documentationUrl(),
                project.architectureUrl(),
                project.stacks(),
                splitLines(fields.getOrDefault("features", joinLines(project.features()))),
                project.links(),
                project.featured(),
                project.published(),
                project.displayOrder(),
                project.slug(),
                project.proofTags(),
                project.caseStudy()
        );
    }

    private ProfileResponseDTO localizeProfile(
            ProfileResponseDTO profile,
            TranslationStoreService.PublishedTranslations publishedTranslations
    ) {
        if (profile == null) return null;
        Map<String, String> fields = translations.publishedFields(
                publishedTranslations,
                TranslationContentType.PROFILE,
                String.valueOf(profile.id()),
                profileSourceFields(profile)
        );
        return new ProfileResponseDTO(
                profile.id(),
                fields.getOrDefault("title", profile.title()),
                fields.getOrDefault("subtitle", profile.subtitle()),
                fields.getOrDefault("headline", profile.headline()),
                fields.getOrDefault("shortDescription", profile.shortDescription()),
                fields.getOrDefault("description", profile.description()),
                fields.getOrDefault("location", profile.location()),
                fields.getOrDefault("availability", profile.availability()),
                profile.profileImageUrl(),
                profile.logoUrl(),
                profile.cvUrl(),
                profile.portfolioUrl(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    private TimelineResponseDTO localizeTimeline(
            TimelineResponseDTO timeline,
            TranslationStoreService.PublishedTranslations publishedTranslations
    ) {
        if (timeline == null) return null;
        Map<String, String> fields = translations.publishedFields(
                publishedTranslations,
                TranslationContentType.TIMELINE,
                String.valueOf(timeline.id()),
                timelineSourceFields(timeline)
        );
        return new TimelineResponseDTO(
                timeline.id(),
                fields.getOrDefault("title", timeline.title()),
                fields.getOrDefault("description", timeline.description()),
                timeline.experiences().stream()
                        .map(experience -> localizeExperience(experience, publishedTranslations))
                        .toList()
        );
    }

    private ExperienceResponseDTO localizeExperience(
            ExperienceResponseDTO experience,
            TranslationStoreService.PublishedTranslations publishedTranslations
    ) {
        Map<String, String> fields = translations.publishedFields(
                publishedTranslations,
                TranslationContentType.EXPERIENCE,
                String.valueOf(experience.id()),
                experienceSourceFields(experience)
        );
        return new ExperienceResponseDTO(
                experience.id(),
                experience.category(),
                fields.getOrDefault("title", experience.title()),
                experience.organization(),
                fields.getOrDefault("location", experience.location()),
                fields.getOrDefault("summary", experience.summary()),
                fields.getOrDefault("description", experience.description()),
                experience.startDate(),
                experience.endDate(),
                experience.currentPosition(),
                experience.imageUrl(),
                experience.websiteUrl(),
                experience.skills(),
                experience.displayOrder()
        );
    }

    private static Map<String, String> profileSourceFields(ProfileResponseDTO profile) {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "title", profile.title());
        put(fields, "subtitle", profile.subtitle());
        put(fields, "headline", profile.headline());
        put(fields, "shortDescription", profile.shortDescription());
        put(fields, "description", profile.description());
        put(fields, "location", profile.location());
        put(fields, "availability", profile.availability());
        return fields;
    }

    private static Map<String, String> timelineSourceFields(TimelineResponseDTO timeline) {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "title", timeline.title());
        put(fields, "description", timeline.description());
        return fields;
    }

    private static Map<String, String> experienceSourceFields(ExperienceResponseDTO experience) {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "title", experience.title());
        put(fields, "location", experience.location());
        put(fields, "summary", experience.summary());
        put(fields, "description", experience.description());
        return fields;
    }

    private static Map<String, String> projectSourceFields(ProjectResponseDTO project) {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "title", project.title());
        put(fields, "subtitle", project.subtitle());
        put(fields, "shortDescription", project.shortDescription());
        put(fields, "description", project.description());
        if (project.features() != null && !project.features().isEmpty()) {
            put(fields, "features", joinLines(project.features()));
        }
        return fields;
    }

    private static void put(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) fields.put(key, value);
    }

    private static String joinLines(List<String> values) {
        return values == null ? "" : String.join("\n", values);
    }

    private static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
