package sorbonne.professional_website.service;

import org.junit.jupiter.api.Test;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;
import sorbonne.professional_website.entity.enumerations.ProjectLinkType;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebsiteVersionClonerTest {

    private final WebsiteVersionCloner cloner = new WebsiteVersionCloner();

    @Test
    void deepCopiesTimelineCollectionsAndBackReferences() {
        Timeline source = Timeline.builder().title("Parcours").description("D").build();
        Experience experience = Experience.builder()
                .category(CategoryExperience.CDI)
                .title("Dev")
                .startDate(LocalDate.of(2026, 1, 1))
                .skills(new ArrayList<>(List.of("Java", "SQL")))
                .timeline(source)
                .build();
        source.getExperiences().add(experience);

        Timeline copy = cloner.copyTimeline(source);

        assertThat(copy).isNotSameAs(source);
        assertThat(copy.getExperiences()).hasSize(1);
        assertThat(copy.getExperiences().getFirst()).isNotSameAs(experience);
        assertThat(copy.getExperiences().getFirst().getTimeline()).isSameAs(copy);
        assertThat(copy.getExperiences().getFirst().getSkills()).isEqualTo(List.of("Java", "SQL"));
        assertThat(copy.getExperiences().getFirst().getSkills()).isNotSameAs(experience.getSkills());
    }

    @Test
    void deepCopiesProjectCollectionsAndLinks() {
        Project source = Project.builder()
                .title("Portfolio")
                .description("D")
                .status(ProjectStatus.IN_PROGRESS)
                .stacks(new ArrayList<>(List.of("React")))
                .features(new ArrayList<>(List.of("E2E")))
                .architectureUrl("https://example.test/architecture")
                .slug("portfolio")
                .proofTags(new ArrayList<>(List.of("Java")))
                .caseStudyProblem("Problem")
                .caseStudyTechnicalChoices(new ArrayList<>(List.of("Choice")))
                .links(new ArrayList<>(List.of(Project.ProjectLink.builder()
                        .type(ProjectLinkType.GITHUB)
                        .label("Code")
                        .url("https://example.test/code")
                        .build())))
                .featured(true)
                .published(true)
                .build();

        Project copy = cloner.copyProjects(List.of(source)).getFirst();

        assertThat(copy).isNotSameAs(source);
        assertThat(copy.getStacks()).containsExactly("React").isNotSameAs(source.getStacks());
        assertThat(copy.getFeatures()).containsExactly("E2E").isNotSameAs(source.getFeatures());
        assertThat(copy.getArchitectureUrl()).isEqualTo("https://example.test/architecture");
        assertThat(copy.getSlug()).isEqualTo("portfolio");
        assertThat(copy.getProofTags()).containsExactly("Java").isNotSameAs(source.getProofTags());
        assertThat(copy.getCaseStudyProblem()).isEqualTo("Problem");
        assertThat(copy.getCaseStudyTechnicalChoices()).containsExactly("Choice")
                .isNotSameAs(source.getCaseStudyTechnicalChoices());
        assertThat(copy.getLinks()).hasSize(1).isNotSameAs(source.getLinks());
        assertThat(copy.getLinks().getFirst()).isNotSameAs(source.getLinks().getFirst());
    }
}
