package sorbonne.professional_website.publication;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.translation.service.PortfolioLocalizationService;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicationPreviewServiceTest {

    @Test
    void buildsPrivateSnapshotFromSelectedDraftWithoutChangingPublicSelection() {
        OwnerRepository owners = mock(OwnerRepository.class);
        WebsiteVersionRepository versions = mock(WebsiteVersionRepository.class);
        PortfolioLocalizationService localization = mock(PortfolioLocalizationService.class);
        PublicationPreviewService service = new PublicationPreviewService(owners, versions, localization);

        Owner owner = Owner.builder()
                .ownerId(1L)
                .rowVersion(7L)
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .active(true)
                .address("Paris")
                .build();
        Project visible = Project.builder().id(10L).title("Visible").published(true).build();
        Project hidden = Project.builder().id(11L).title("Hidden").published(false).build();
        WebsiteVersion draft = WebsiteVersion.builder()
                .id(2L)
                .versionTag("v2")
                .label("Draft")
                .owner(owner)
                .projects(new ArrayList<>())
                .build();
        draft.addProject(visible);
        draft.addProject(hidden);

        when(owners.findById(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(2L, 1L)).thenReturn(Optional.of(draft));
        when(localization.localizeSnapshot(org.mockito.ArgumentMatchers.any(OwnerResponseDTO.class), eq("en")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnerResponseDTO response = service.preview(1L, 2L, "en");

        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.websiteVersions()).hasSize(1);
        assertThat(response.websiteVersions().getFirst().id()).isEqualTo(2L);
        assertThat(response.projects()).extracting(project -> project.title()).containsExactly("Visible");

        ArgumentCaptor<OwnerResponseDTO> snapshot = ArgumentCaptor.forClass(OwnerResponseDTO.class);
        verify(localization).localizeSnapshot(snapshot.capture(), eq("en"));
        assertThat(snapshot.getValue().websiteVersions()).hasSize(1);
        verify(versions).findByIdAndOwnerOwnerId(2L, 1L);
    }
}
