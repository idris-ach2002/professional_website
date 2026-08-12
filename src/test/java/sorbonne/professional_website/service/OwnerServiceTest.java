package sorbonne.professional_website.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.cache.PortfolioChangePublisher;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.repository.OwnerRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock OwnerRepository ownerRepository;
    @Mock PortfolioChangePublisher changePublisher;

    @Test
    void staleOwnerRevisionCannotOverwriteNewerState() {
        Owner owner = Owner.builder()
                .ownerId(7L).rowVersion(4L)
                .name("ACHABOU").firstName("Idris").age(24).active(true).address("Paris")
                .build();
        when(ownerRepository.lockByOwnerId(7L)).thenReturn(Optional.of(owner));
        OwnerService service = new OwnerService(ownerRepository, changePublisher);

        OwnerRequestDTO request = new OwnerRequestDTO(
                "ACHABOU", "Idris", 25, true, "Paris", List.of(),
                null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.updateOwner(7L, 3L, request))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessageContaining("attendu=3")
                .hasMessageContaining("courant=4");
        verify(ownerRepository, never()).saveAndFlush(owner);
    }

    @Test
    void matchingOwnerRevisionAllowsUpdate() {
        Owner owner = Owner.builder()
                .ownerId(7L).rowVersion(4L)
                .name("ACHABOU").firstName("Idris").age(24).active(true).address("Paris")
                .build();
        when(ownerRepository.lockByOwnerId(7L)).thenReturn(Optional.of(owner));
        when(ownerRepository.saveAndFlush(owner)).thenReturn(owner);
        OwnerService service = new OwnerService(ownerRepository, changePublisher);

        OwnerRequestDTO request = new OwnerRequestDTO(
                "ACHABOU", "Idris", 25, true, "Nanterre", List.of(),
                null, null, null, null, null, null, null
        );

        var response = service.updateOwner(7L, 4L, request);
        assertThat(response.age()).isEqualTo(25);
        assertThat(response.address()).isEqualTo("Nanterre");
        verify(changePublisher).changed(7L, "owner-updated");
    }
}
