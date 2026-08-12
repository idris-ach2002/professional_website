package sorbonne.professional_website.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;
import sorbonne.professional_website.entity.WebsiteVersion;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioBackupCodecTest {

    private final PortfolioBackupCodec codec = new PortfolioBackupCodec(new ObjectMapper().findAndRegisterModules(), new PortfolioHealthEvaluator());

    @Test
    void backupRoundTripPreservesVersionRequestAndProducesPortableZip() throws Exception {
        Owner owner = Owner.builder().ownerId(1L).name("ACHABOU").firstName("Idris").age(24).address("Paris").build();
        WebsiteVersion version = WebsiteVersion.builder()
                .id(8L).owner(owner).versionTag("V 8").label("Release").description("snapshot")
                .active(true).published(true).build();
        version.clearAndAttachProjects(java.util.List.of(Project.builder()
                .title("Ocean")
                .description("Project")
                .status(ProjectStatus.IN_PROGRESS)
                .architectureUrl("https://example.test/architecture")
                .slug("ocean")
                .proofTags(java.util.List.of("Java"))
                .caseStudyProblem("Problem")
                .caseStudyTechnicalChoices(java.util.List.of("Choice"))
                .featured(true)
                .published(true)
                .build()));

        PortfolioBackupCodec.BackupArtifact artifact = codec.encode(1L, version);
        WebsiteVersionRequestDTO restored = codec.decodeVersionRequest(artifact.json());

        assertThat(artifact.filename()).isEqualTo("portfolio-backup-v-8.zip");
        assertThat(restored.versionTag()).isEqualTo("V 8");
        assertThat(restored.label()).isEqualTo("Release");
        assertThat(restored.active()).isFalse();
        assertThat(restored.published()).isTrue();
        assertThat(restored.projects()).hasSize(1);
        assertThat(restored.projects().getFirst().slug()).isEqualTo("ocean");
        assertThat(restored.projects().getFirst().proofTags()).containsExactly("Java");
        assertThat(restored.projects().getFirst().caseStudy().problem()).isEqualTo("Problem");
        assertThat(artifact.json()).contains("\"format\" : \"portfolio-backup-v1\"");

        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(artifact.zipBytes()), StandardCharsets.UTF_8)) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("portfolio.json");
            assertThat(zip.getNextEntry().getName()).isEqualTo("metadata.json");
        }
    }

    @Test
    void invalidBackupFailsWithoutPartialRecovery() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> codec.decodeVersionRequest("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Backup JSON illisible");
    }
}
