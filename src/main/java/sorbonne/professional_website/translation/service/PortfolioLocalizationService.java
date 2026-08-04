package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.dto.response.*;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.translation.entity.TranslationContentType;

import java.util.Arrays;
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
        OwnerResponseDTO source = OwnerMapper.toResponse(owner);
        String locale = localeNormalizer.normalize(requestedLocale);

        ProfileResponseDTO profile = localizeProfile(source.prof(), locale);
        TimelineResponseDTO timeline = localizeTimeline(source.timeline(), locale);
        List<ProjectResponseDTO> projects = source.projects().stream()
                .map(project -> localizeProject(project, locale))
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
                locale
        );

        return new OwnerResponseDTO(
                source.ownerId(),
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
        Map<String, String> fields = translations.publishedFields(
                TranslationContentType.PROJECT,
                String.valueOf(project.id()),
                locale
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
                project.stacks(),
                splitLines(fields.getOrDefault("features", joinLines(project.features()))),
                project.links(),
                project.featured(),
                project.published(),
                project.displayOrder(),
                project.slug()
        );
    }

    private ProfileResponseDTO localizeProfile(ProfileResponseDTO profile, String locale) {
        if (profile == null) return null;
        Map<String, String> fields = translations.publishedFields(
                TranslationContentType.PROFILE,
                String.valueOf(profile.id()),
                locale
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

    private TimelineResponseDTO localizeTimeline(TimelineResponseDTO timeline, String locale) {
        if (timeline == null) return null;
        Map<String, String> fields = translations.publishedFields(
                TranslationContentType.TIMELINE,
                String.valueOf(timeline.id()),
                locale
        );
        return new TimelineResponseDTO(
                timeline.id(),
                fields.getOrDefault("title", timeline.title()),
                fields.getOrDefault("description", timeline.description()),
                timeline.experiences().stream().map(experience -> localizeExperience(experience, locale)).toList()
        );
    }

    private ExperienceResponseDTO localizeExperience(ExperienceResponseDTO experience, String locale) {
        Map<String, String> fields = translations.publishedFields(
                TranslationContentType.EXPERIENCE,
                String.valueOf(experience.id()),
                locale
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
