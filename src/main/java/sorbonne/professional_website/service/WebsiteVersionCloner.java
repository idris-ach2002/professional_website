package sorbonne.professional_website.service;

import org.springframework.stereotype.Component;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a detached deep copy of version-owned content.
 *
 * <p>The clone never reuses mutable collection instances or back-references
 * from the source persistence graph. The target WebsiteVersion is responsible
 * for attaching the returned content to its own aggregate.</p>
 */
@Component
public class WebsiteVersionCloner {

    public Profile copyProfile(Profile source) {
        if (source == null) return null;

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

    public Timeline copyTimeline(Timeline source) {
        if (source == null) return null;

        Timeline copy = Timeline.builder()
                .title(source.getTitle())
                .description(source.getDescription())
                .build();

        if (source.getExperiences() != null) {
            for (Experience experience : source.getExperiences()) {
                Experience experienceCopy = Experience.builder()
                        .category(experience.getCategory())
                        .title(experience.getTitle())
                        .organization(experience.getOrganization())
                        .location(experience.getLocation())
                        .summary(experience.getSummary())
                        .description(experience.getDescription())
                        .startDate(experience.getStartDate())
                        .endDate(experience.getEndDate())
                        .currentPosition(experience.isCurrentPosition())
                        .imageUrl(experience.getImageUrl())
                        .websiteUrl(experience.getWebsiteUrl())
                        .skills(copyStrings(experience.getSkills()))
                        .displayOrder(experience.getDisplayOrder())
                        .timeline(copy)
                        .build();
                copy.getExperiences().add(experienceCopy);
            }
        }

        return copy;
    }

    public List<Project> copyProjects(List<Project> sourceProjects) {
        if (sourceProjects == null) return new ArrayList<>();

        List<Project> copies = new ArrayList<>(sourceProjects.size());
        for (Project project : sourceProjects) {
            copies.add(Project.builder()
                    .title(project.getTitle())
                    .subtitle(project.getSubtitle())
                    .shortDescription(project.getShortDescription())
                    .description(project.getDescription())
                    .status(project.getStatus())
                    .startDate(project.getStartDate())
                    .endDate(project.getEndDate())
                    .imageUrl(project.getImageUrl())
                    .demoUrl(project.getDemoUrl())
                    .githubUrl(project.getGithubUrl())
                    .documentationUrl(project.getDocumentationUrl())
                    .architectureUrl(project.getArchitectureUrl())
                    .slug(project.getSlug())
                    .stacks(copyStrings(project.getStacks()))
                    .features(copyStrings(project.getFeatures()))
                    .proofTags(copyStrings(project.getProofTags()))
                    .caseStudyProblem(project.getCaseStudyProblem())
                    .caseStudyContext(project.getCaseStudyContext())
                    .caseStudyRole(project.getCaseStudyRole())
                    .caseStudyArchitecture(project.getCaseStudyArchitecture())
                    .caseStudyNextSteps(project.getCaseStudyNextSteps())
                    .caseStudyTechnicalChoices(copyStrings(project.getCaseStudyTechnicalChoices()))
                    .caseStudyChallenges(copyStrings(project.getCaseStudyChallenges()))
                    .caseStudySolutions(copyStrings(project.getCaseStudySolutions()))
                    .caseStudyOutcomes(copyStrings(project.getCaseStudyOutcomes()))
                    .caseStudyResults(copyStrings(project.getCaseStudyResults()))
                    .caseStudyLimits(copyStrings(project.getCaseStudyLimits()))
                    .links(copyProjectLinks(project.getLinks()))
                    .featured(project.getFeatured())
                    .published(project.getPublished())
                    .displayOrder(project.getDisplayOrder())
                    .build());
        }
        return copies;
    }

    private List<Project.ProjectLink> copyProjectLinks(List<Project.ProjectLink> sourceLinks) {
        if (sourceLinks == null) return new ArrayList<>();

        List<Project.ProjectLink> copies = new ArrayList<>(sourceLinks.size());
        for (Project.ProjectLink link : sourceLinks) {
            copies.add(Project.ProjectLink.builder()
                    .type(link.getType())
                    .label(link.getLabel())
                    .url(link.getUrl())
                    .build());
        }
        return copies;
    }

    private List<String> copyStrings(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
