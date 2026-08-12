package sorbonne.professional_website.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.translation.service.PortfolioLocalizationService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebsiteServiceTest {

    @Mock
    private OwnerRepository ownerRepository;
    @Mock
    private PortfolioLocalizationService localizationService;

    private WebsiteService service;

    @BeforeEach
    void setUp() {
        service = new WebsiteService(ownerRepository, localizationService);
    }

    @Test
    void defaultWebsiteUsesTheFirstActivePublishedOwner() {
        Owner owner = new Owner();
        when(ownerRepository.findAllPublicOwners()).thenReturn(List.of(owner));

        service.getFirstOwner("en");

        verify(localizationService).localizePublic(owner, "en");
    }

    @Test
    void missingDefaultPublicOwnerProducesAResourceNotFoundError() {
        when(ownerRepository.findAllPublicOwners()).thenReturn(List.of());

        assertThatThrownBy(() -> service.getFirstOwner("fr"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Owner introuvable.");
    }

    @Test
    void ownerLookupRejectsInactiveOrUnpublishedContentAtRepositoryBoundary() {
        when(ownerRepository.findPublicOwnerById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicWebsiteByOwnerId(99L, "fr"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void seoSnapshotUsesTheSamePublicOwnerForFrenchAndEnglish() {
        Owner owner = new Owner();
        OwnerResponseDTO fr = ownerDto("fr");
        OwnerResponseDTO en = ownerDto("en");
        when(ownerRepository.findAllPublicOwners()).thenReturn(List.of(owner));
        when(localizationService.localizePublic(owner, "fr")).thenReturn(fr);
        when(localizationService.localizePublic(owner, "en")).thenReturn(en);

        var snapshot = service.getPublicSeoSnapshot();

        assertThat(snapshot.generatedAt()).isNotNull();
        assertThat(snapshot.fr()).isSameAs(fr);
        assertThat(snapshot.en()).isSameAs(en);
        verify(localizationService).localizePublic(owner, "fr");
        verify(localizationService).localizePublic(owner, "en");
    }


    @Test
    void publicProjectLookupUsesPersistedSlugInsteadOfRecomputingItFromTheTitle() {
        Project project = Project.builder()
                .id(7L)
                .title("Titre complètement différent")
                .description("Description")
                .slug("architecture-concurrente")
                .published(true)
                .build();
        WebsiteVersion version = WebsiteVersion.builder()
                .active(true)
                .published(true)
                .build();
        version.addProject(project);
        Owner owner = Owner.builder().websiteVersions(List.of(version)).build();
        ProjectResponseDTO localized = new ProjectResponseDTO(
                7L, "Titre", null, null, "Description", null, null, null,
                null, null, null, null, null, List.of(), List.of(), List.of(),
                false, true, 1, "architecture-concurrente", List.of(), null
        );

        when(ownerRepository.findAllPublicOwners()).thenReturn(List.of(owner));
        when(localizationService.localizeProject(
                argThat(dto -> dto != null && "architecture-concurrente".equals(dto.slug())),
                eq("fr")
        )).thenReturn(localized);

        assertThat(service.getDefaultProjectBySlug("architecture-concurrente", "fr")).isSameAs(localized);
    }

    private static OwnerResponseDTO ownerDto(String locale) {
        return new OwnerResponseDTO(
                1L, 0L, "ACHABOU", "Idris", 24, true, "Paris",
                List.of(), null, null, List.of(), List.of(), locale, List.of()
        );
    }
}
