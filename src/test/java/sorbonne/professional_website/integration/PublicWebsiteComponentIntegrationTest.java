package sorbonne.professional_website.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.service.WebsiteService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PublicWebsiteComponentIntegrationTest {

    @Autowired OwnerRepository ownerRepository;
    @Autowired WebsiteService websiteService;

    @BeforeEach
    void clean() {
        ownerRepository.deleteAll();
    }

    @Test
    void publicReadModelOnlyExposesActivePublishedVersionAndPublishedProjects() {
        Owner owner = Owner.builder()
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .address("Paris")
                .active(true)
                .build();
        WebsiteVersion version = WebsiteVersion.builder()
                .versionTag("v1")
                .label("Production")
                .active(true)
                .published(true)
                .owner(owner)
                .build();
        version.addProject(Project.builder()
                .title("Published")
                .description("Visible")
                .status(ProjectStatus.COMPLETED)
                .published(true)
                .build());
        version.addProject(Project.builder()
                .title("Draft")
                .description("Hidden")
                .status(ProjectStatus.IN_PROGRESS)
                .published(false)
                .build());
        owner.getWebsiteVersions().add(version);
        ownerRepository.saveAndFlush(owner);

        var response = websiteService.getFirstOwner("fr");

        assertThat(response.projects()).extracting(project -> project.title()).containsExactly("Published");
        assertThat(response.websiteVersions()).hasSize(1);
    }
}
