package sorbonne.professional_website.mapper;

import org.junit.jupiter.api.Test;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioMappersTest {

    @Test
    void publicOwnerMappingKeepsOnlyPublishedContentFromActivePublishedVersion() {
        Owner owner = Owner.builder()
                .ownerId(7L)
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .address("Paris")
                .active(true)
                .build();

        WebsiteVersion version = WebsiteVersion.builder()
                .id(11L)
                .versionTag("v22")
                .label("Production")
                .active(true)
                .published(true)
                .owner(owner)
                .build();
        owner.getWebsiteVersions().add(version);

        version.attachProfile(Profile.builder()
                .id(1L)
                .title("Full Stack")
                .description("Profile")
                .build());

        Timeline timeline = Timeline.builder().id(2L).title("Parcours").build();
        Experience experience = Experience.builder()
                .id(3L)
                .category(CategoryExperience.INTERNSHIP)
                .title("LITIS")
                .startDate(LocalDate.of(2025, 4, 1))
                .skills(List.of("Java", "PostgreSQL"))
                .build();
        experience.setTimeline(timeline);
        timeline.getExperiences().add(experience);
        version.attachTimeline(timeline);

        version.addProject(Project.builder()
                .id(4L)
                .title("Projet publié")
                .description("Visible")
                .status(ProjectStatus.COMPLETED)
                .published(true)
                .stacks(List.of("Spring", "React"))
                .features(List.of("Cache"))
                .build());
        version.addProject(Project.builder()
                .id(5L)
                .title("Brouillon")
                .description("Invisible")
                .status(ProjectStatus.IN_PROGRESS)
                .published(false)
                .build());

        var response = OwnerMapper.toPublicResponse(owner);

        assertThat(response.firstName()).isEqualTo("Idris");
        assertThat(response.prof().title()).isEqualTo("Full Stack");
        assertThat(response.timeline().experiences()).singleElement()
                .satisfies(item -> assertThat(item.skills()).containsExactly("Java", "PostgreSQL"));
        assertThat(response.projects()).singleElement()
                .satisfies(project -> {
                    assertThat(project.title()).isEqualTo("Projet publié");
                    assertThat(project.slug()).isEqualTo("projet-publie");
                    assertThat(project.stacks()).containsExactly("Spring", "React");
                });
        assertThat(response.websiteVersions()).hasSize(1);
    }

    @Test
    void mapperNullAndCollectionGuardsStayDeterministic() {
        assertThat(ProfileMapper.toResponse(null)).isNull();
        assertThat(TimelineMapper.toResponse(null)).isNull();
        assertThat(ProjectMapper.toResponse(null)).isNull();
        assertThat(ExperienceMapper.toResponse(null)).isNull();
        assertThat(WebsiteVersionMapper.toResponse(null)).isNull();
        assertThat(OwnerMapper.toPublicResponse(null)).isNull();
        assertThat(ProjectMapper.toResponseList(null)).isEmpty();
        assertThat(ExperienceMapper.toResponseList(null)).isEmpty();
    }
}
