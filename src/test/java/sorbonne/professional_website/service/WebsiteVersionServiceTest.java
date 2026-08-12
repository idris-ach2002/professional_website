package sorbonne.professional_website.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.cache.PortfolioChangePublisher;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.ProjectRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.upload.StorageService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebsiteVersionServiceTest {

    @Mock OwnerRepository ownerRepository;
    @Mock WebsiteVersionRepository versionRepository;
    @Mock ProjectRepository projectRepository;
    @Mock StorageService storageService;
    @Mock PortfolioChangePublisher changePublisher;
    @Mock PortfolioHealthEvaluator healthEvaluator;
    @Mock PortfolioBackupCodec backupCodec;
    @Mock WebsiteVersionCloner versionCloner;

    private WebsiteVersionService service;

    @BeforeEach
    void setUp() {
        service = new WebsiteVersionService(
                ownerRepository,
                versionRepository,
                projectRepository,
                storageService,
                changePublisher,
                healthEvaluator,
                backupCodec,
                versionCloner
        );
    }

    @Test
    void firstCreatedVersionIsActivatedAndPublishedByDefault() {
        Owner owner = owner();
        when(ownerRepository.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versionRepository.existsByOwnerOwnerIdAndActiveTrue(1L)).thenReturn(false);
        when(versionRepository.countByOwnerOwnerId(1L)).thenReturn(0L);
        when(versionRepository.save(any(WebsiteVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createVersion(1L, new WebsiteVersionRequestDTO(null, null, null, null, null, null, null, null));

        assertThat(result.active()).isTrue();
        assertThat(result.published()).isTrue();
        assertThat(result.versionTag()).isEqualTo("v1");
        verify(versionRepository).deactivateAllByOwnerId(1L);
        verify(changePublisher).changed(1L, "version-created");
    }

    @Test
    void activationSerializesOnOwnerLockAndPublishesTargetVersion() {
        Owner owner = owner();
        WebsiteVersion target = WebsiteVersion.builder()
                .id(20L).versionTag("v2").label("V2").active(false).published(false).owner(owner).build();
        when(ownerRepository.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versionRepository.findByIdAndOwnerOwnerId(20L, 1L)).thenReturn(Optional.of(target));
        when(versionRepository.saveAndFlush(target)).thenReturn(target);

        var result = service.activateVersion(1L, 20L, 0L);

        assertThat(result.active()).isTrue();
        assertThat(result.published()).isTrue();
        verify(versionRepository).deactivateOthersByOwnerId(1L, 20L);
        verify(changePublisher).changed(1L, "version-activated");
    }


    @Test
    void reactivatingCurrentVersionDoesNotBulkDeactivateItsManagedRow() {
        Owner owner = owner();
        WebsiteVersion target = WebsiteVersion.builder()
                .id(20L).versionTag("v2").label("V2").active(true).published(true).owner(owner).build();
        when(ownerRepository.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versionRepository.findByIdAndOwnerOwnerId(20L, 1L)).thenReturn(Optional.of(target));
        when(versionRepository.saveAndFlush(target)).thenReturn(target);

        var result = service.activateVersion(1L, 20L, 0L);

        assertThat(result.active()).isTrue();
        verify(versionRepository).deactivateOthersByOwnerId(1L, 20L);
    }

    @Test
    void activeVersionCannotBeDeleted() {
        Owner owner = owner();
        WebsiteVersion target = WebsiteVersion.builder()
                .id(20L).versionTag("v2").label("V2").active(true).published(true).owner(owner).build();
        when(ownerRepository.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versionRepository.findByIdAndOwnerOwnerId(20L, 1L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.deleteVersion(1L, 20L, 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version active");
    }


    @Test
    void staleContentRevisionCannotOverwriteVersion() {
        Owner owner = owner();
        WebsiteVersion target = WebsiteVersion.builder()
                .id(20L).contentRevision(3L).versionTag("v2").label("V2").active(false).published(false).owner(owner).build();
        when(ownerRepository.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versionRepository.findByIdAndOwnerOwnerId(20L, 1L)).thenReturn(Optional.of(target));

        WebsiteVersionRequestDTO request = new WebsiteVersionRequestDTO(
                "v2", "Nouvelle étiquette", null, false, false, null, null, null
        );

        assertThatThrownBy(() -> service.updateVersion(1L, 20L, 2L, request))
                .isInstanceOf(sorbonne.professional_website.exception.PreconditionFailedException.class)
                .hasMessageContaining("attendu=2")
                .hasMessageContaining("courant=3");
    }

    @Test
    void successfulMutationBumpsContentRevisionExactlyOnce() {
        Owner owner = owner();
        WebsiteVersion target = WebsiteVersion.builder()
                .id(20L).contentRevision(3L).versionTag("v2").label("V2").active(false).published(false).owner(owner).build();
        when(ownerRepository.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versionRepository.findByIdAndOwnerOwnerId(20L, 1L)).thenReturn(Optional.of(target));
        when(versionRepository.saveAndFlush(target)).thenReturn(target);

        WebsiteVersionRequestDTO request = new WebsiteVersionRequestDTO(
                "v2", "Nouvelle étiquette", null, false, false, null, null, null
        );

        var result = service.updateVersion(1L, 20L, 3L, request);

        assertThat(result.contentRevision()).isEqualTo(4L);
        assertThat(result.label()).isEqualTo("Nouvelle étiquette");
    }

    private static Owner owner() {
        return Owner.builder().ownerId(1L).name("ACHABOU").firstName("Idris").age(24).address("Paris").build();
    }
}
