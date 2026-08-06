package sorbonne.professional_website.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.translation.service.PortfolioLocalizationService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void defaultWebsiteUsesTheFirstOwner() {
        Owner owner = new Owner();
        when(ownerRepository.findFirstByOrderByOwnerIdAsc()).thenReturn(Optional.of(owner));

        service.getFirstOwner("en");

        verify(localizationService).localize(owner, "en");
    }

    @Test
    void missingDefaultOwnerProducesAResourceNotFoundError() {
        when(ownerRepository.findFirstByOrderByOwnerIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFirstOwner("fr"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Owner introuvable.");
    }
}
