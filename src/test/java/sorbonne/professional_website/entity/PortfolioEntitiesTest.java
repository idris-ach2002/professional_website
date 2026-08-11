package sorbonne.professional_website.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioEntitiesTest {

    @Test
    void ownerSeparatesActiveAdminVersionFromPublishedPublicVersion() {
        WebsiteVersion draft = WebsiteVersion.builder().versionTag("draft").label("Draft").active(true).published(false).build();
        WebsiteVersion published = WebsiteVersion.builder().versionTag("public").label("Public").active(false).published(true).build();
        Owner owner = Owner.builder().name("A").firstName("I").age(24).address("Paris").websiteVersions(List.of(draft, published)).build();

        assertThat(owner.getActiveWebsiteVersion()).containsSame(draft);
        assertThat(owner.getActivePublishedWebsiteVersion()).isEmpty();

        draft.setPublished(true);
        assertThat(owner.getActivePublishedWebsiteVersion()).containsSame(draft);
    }

    @Test
    void websiteVersionAttachmentMaintainsOwningSides() {
        WebsiteVersion version = WebsiteVersion.builder().versionTag("v1").label("V1").build();
        Profile profile = Profile.builder().title("Dev").description("Portfolio").build();
        Timeline timeline = Timeline.builder().title("Parcours").build();
        Project project = Project.builder().title("P").description("D").build();

        version.attachProfile(profile);
        version.attachTimeline(timeline);
        version.addProject(project);

        assertThat(profile.getWebsiteVersion()).isSameAs(version);
        assertThat(timeline.getWebsiteVersion()).isSameAs(version);
        assertThat(project.getWebsiteVersion()).isSameAs(version);
        assertThat(version.getProjects()).containsExactly(project);
    }

    @Test
    void replacingProjectsReattachesEveryProjectAndHandlesNull() {
        WebsiteVersion version = WebsiteVersion.builder().versionTag("v1").label("V1").build();
        Project first = Project.builder().title("A").description("A").build();
        Project second = Project.builder().title("B").description("B").build();

        version.clearAndAttachProjects(List.of(first, second));
        assertThat(version.getProjects()).containsExactly(first, second);
        assertThat(first.getWebsiteVersion()).isSameAs(version);
        assertThat(second.getWebsiteVersion()).isSameAs(version);

        version.clearAndAttachProjects(null);
        assertThat(version.getProjects()).isEmpty();
    }
}
