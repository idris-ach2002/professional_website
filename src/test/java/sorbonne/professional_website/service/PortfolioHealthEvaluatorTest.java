package sorbonne.professional_website.service;

import org.junit.jupiter.api.Test;
import sorbonne.professional_website.entity.ContactInfo;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.entity.enumerations.Contact;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioHealthEvaluatorTest {

    private final PortfolioHealthEvaluator evaluator = new PortfolioHealthEvaluator();

    @Test
    void completePortfolioHasNoPublicationBlocker() {
        ContactInfo email = new ContactInfo();
        email.setType(Contact.EMAIL);
        email.setValue("idris@example.test");
        Owner owner = Owner.builder()
                .ownerId(1L).name("ACHABOU").firstName("Idris").age(24).address("Paris")
                .contacts(java.util.List.of(email)).build();
        WebsiteVersion version = WebsiteVersion.builder()
                .id(2L).owner(owner).versionTag("v1").label("Production").published(true).active(true).build();
        version.attachProfile(Profile.builder().title("Développeur").description("Portfolio").build());
        Timeline timeline = Timeline.builder().title("Parcours").build();
        timeline.getExperiences().add(Experience.builder().title("Sorbonne").startDate(LocalDate.of(2025, 9, 1)).timeline(timeline).build());
        version.attachTimeline(timeline);
        version.addProject(Project.builder().title("Portfolio").published(true).featured(true).imageUrl("/image.webp").githubUrl("https://example.test").build());

        var report = evaluator.evaluate(1L, 2L, version);

        assertThat(report.publishable()).isTrue();
        assertThat(report.blockersCount()).isZero();
        assertThat(report.score()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void missingCriticalContentBlocksPublication() {
        Owner owner = Owner.builder().ownerId(1L).name("A").firstName("I").age(24).address("Paris").build();
        WebsiteVersion version = WebsiteVersion.builder().id(2L).owner(owner).versionTag("v1").label("Draft").build();

        var report = evaluator.evaluate(1L, 2L, version);

        assertThat(report.publishable()).isFalse();
        assertThat(report.blockersCount()).isGreaterThanOrEqualTo(4);
        assertThat(report.checks()).anyMatch(check -> "profile.title".equals(check.id()) && "FAIL".equals(check.status()));
    }
}
