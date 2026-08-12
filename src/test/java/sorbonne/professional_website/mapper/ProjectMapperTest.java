package sorbonne.professional_website.mapper;

import org.junit.jupiter.api.Test;
import sorbonne.professional_website.dto.request.ProjectCaseStudyRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMapperTest {

    @Test
    void persistsAndExposesTheCompleteAdminProjectContract() {
        ProjectRequestDTO request = new ProjectRequestDTO(
                "Portfolio Océan",
                "Sous-titre",
                "Résumé",
                "Description",
                ProjectStatus.IN_PROGRESS,
                null,
                null,
                null,
                null,
                null,
                "https://example.test/docs",
                "https://example.test/architecture",
                "portfolio-ocean",
                List.of("React", "Spring"),
                List.of("Tests E2E"),
                List.of("Java", "Concurrence"),
                new ProjectCaseStudyRequestDTO(
                        "Problème", "Contexte", "Rôle", "Architecture",
                        List.of("Choix 1"), List.of("Défi 1"), List.of("Solution 1"),
                        List.of("Impact 1"), List.of("Résultat 1"), List.of("Limite 1"), "Suite"
                ),
                List.of(),
                true,
                true,
                1,
                9L
        );

        Project entity = ProjectMapper.fromRequest(request);
        entity.setId(12L);
        var response = ProjectMapper.toResponse(entity);

        assertThat(entity.getSlug()).isEqualTo("portfolio-ocean");
        assertThat(response.architectureUrl()).isEqualTo("https://example.test/architecture");
        assertThat(response.proofTags()).containsExactly("Java", "Concurrence");
        assertThat(response.caseStudy()).isNotNull();
        assertThat(response.caseStudy().technicalChoices()).containsExactly("Choix 1");
        assertThat(response.caseStudy().nextSteps()).isEqualTo("Suite");
    }

    @Test
    void derivesAndNormalizesSlugWhenTheAdminLeavesItBlank() {
        ProjectRequestDTO request = new ProjectRequestDTO(
                "Éditeur Async & Sûr",
                null,
                null,
                "Description",
                ProjectStatus.IN_PROGRESS,
                null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), null, List.of(), false, true, 1, null
        );

        assertThat(ProjectMapper.normalizedSlug(request)).isEqualTo("editeur-async-sur");
    }
}
