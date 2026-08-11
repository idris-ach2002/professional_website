package sorbonne.professional_website.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.service.WebsiteService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class PublicWebsiteQueryBudgetIntegrationTest {

    private static final long PUBLIC_READ_QUERY_BUDGET = 16L;

    @Autowired OwnerRepository ownerRepository;
    @Autowired WebsiteService websiteService;
    @Autowired CacheManager cacheManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        ownerRepository.deleteAll();

        Owner owner = Owner.builder()
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .address("Paris")
                .active(true)
                .build();
        WebsiteVersion version = WebsiteVersion.builder()
                .versionTag("v-query-budget")
                .label("Production")
                .active(true)
                .published(true)
                .owner(owner)
                .build();
        owner.getWebsiteVersions().add(version);

        Timeline timeline = Timeline.builder().title("Parcours").build();
        for (int index = 0; index < 6; index++) {
            Experience experience = Experience.builder()
                    .category(CategoryExperience.INTERNSHIP)
                    .title("Experience " + index)
                    .startDate(LocalDate.of(2025, 1, 1).plusMonths(index))
                    .skills(new ArrayList<>(List.of("Java", "SQL")))
                    .build();
            experience.setTimeline(timeline);
            timeline.getExperiences().add(experience);
        }
        version.attachTimeline(timeline);

        for (int index = 0; index < 12; index++) {
            version.addProject(Project.builder()
                    .title("Project " + index)
                    .description("Public project")
                    .status(ProjectStatus.COMPLETED)
                    .published(true)
                    .displayOrder(index)
                    .stacks(new ArrayList<>(List.of("Java", "React")))
                    .features(new ArrayList<>(List.of("Feature A", "Feature B")))
                    .build());
        }
        ownerRepository.saveAndFlush(owner);

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void publicPortfolioQueryCountIsBoundedInsteadOfGrowingPerProject() {
        var response = websiteService.getFirstOwner("fr");

        assertThat(response.projects()).hasSize(12);
        assertThat(response.timeline().experiences()).hasSize(6);
        assertThat(statistics.getPrepareStatementCount())
                .as("Public read query budget; protects against N+1 regressions")
                .isLessThanOrEqualTo(PUBLIC_READ_QUERY_BUDGET);
    }
}
