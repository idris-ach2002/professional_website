package sorbonne.professional_website.integration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;
import sorbonne.professional_website.repository.OwnerRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityPersistenceComponentIntegrationTest {

    @Autowired OwnerRepository ownerRepository;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void clean() {
        ownerRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    void aggregatePersistsAndReloadsItsBidirectionalComponentGraph() {
        Owner owner = Owner.builder()
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .address("Paris")
                .active(true)
                .build();

        WebsiteVersion version = WebsiteVersion.builder()
                .versionTag("v22-component")
                .label("V22 component")
                .active(true)
                .published(true)
                .owner(owner)
                .build();
        owner.getWebsiteVersions().add(version);

        version.attachProfile(Profile.builder()
                .title("Développeur Full Stack")
                .description("Profil test")
                .build());

        Timeline timeline = Timeline.builder()
                .title("Parcours")
                .description("Timeline test")
                .build();
        Experience experience = Experience.builder()
                .category(CategoryExperience.INTERNSHIP)
                .title("Stage")
                .description("Backend")
                .startDate(LocalDate.of(2025, 4, 1))
                .skills(List.of("Java", "PostgreSQL"))
                .build();
        experience.setTimeline(timeline);
        timeline.getExperiences().add(experience);
        version.attachTimeline(timeline);

        Project project = Project.builder()
                .title("Portfolio")
                .description("Projet public")
                .status(ProjectStatus.COMPLETED)
                .published(true)
                .featured(true)
                .stacks(List.of("Java", "React"))
                .features(List.of("Cache", "Animations"))
                .build();
        version.addProject(project);

        Owner saved = ownerRepository.saveAndFlush(owner);
        Long ownerId = saved.getOwnerId();
        entityManager.clear();

        Owner reloaded = ownerRepository.findById(ownerId).orElseThrow();
        WebsiteVersion publicVersion = reloaded.getActivePublishedWebsiteVersion().orElseThrow();

        assertThat(publicVersion.getProfile().getWebsiteVersion()).isSameAs(publicVersion);
        assertThat(publicVersion.getTimeline().getWebsiteVersion()).isSameAs(publicVersion);
        assertThat(publicVersion.getTimeline().getExperiences())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getTimeline()).isSameAs(publicVersion.getTimeline());
                    assertThat(item.getSkills()).containsExactly("Java", "PostgreSQL");
                });
        assertThat(publicVersion.getProjects())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getWebsiteVersion()).isSameAs(publicVersion);
                    assertThat(item.getStacks()).containsExactly("Java", "React");
                    assertThat(item.getFeatures()).containsExactly("Cache", "Animations");
                });
    }
}
