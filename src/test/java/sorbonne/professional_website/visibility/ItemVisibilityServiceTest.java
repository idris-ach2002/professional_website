package sorbonne.professional_website.visibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.visibility.entity.FrontItemVisibility;
import sorbonne.professional_website.visibility.repository.FrontItemVisibilityRepository;
import sorbonne.professional_website.visibility.service.ItemVisibilityService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemVisibilityServiceTest {

    @Mock
    private FrontItemVisibilityRepository repository;

    @Test
    void persistsOnlyHiddenOverridesSoNewItemsRemainVisibleByDefault() {
        ItemVisibilityService service = new ItemVisibilityService(repository);
        when(repository.findAll()).thenReturn(List.of(new FrontItemVisibility("architecture.performance", false)));

        Map<String, Boolean> requested = new LinkedHashMap<>();
        requested.put("architecture.system", true);
        requested.put("architecture.performance", false);

        var response = service.replaceOverrides(requested);

        verify(repository).deleteAllInBatch();
        ArgumentCaptor<FrontItemVisibility> captor = ArgumentCaptor.forClass(FrontItemVisibility.class);
        verify(repository).save(captor.capture());
        verify(repository).flush();
        assertThat(captor.getValue().getItemKey()).isEqualTo("architecture.performance");
        assertThat(captor.getValue().isVisible()).isFalse();
        assertThat(response.items()).containsEntry("architecture.performance", false);
        assertThat(response.items()).doesNotContainKey("architecture.system");
    }

    @Test
    void rejectsUnsafeKeys() {
        ItemVisibilityService service = new ItemVisibilityService(repository);
        assertThatThrownBy(() -> service.replaceOverrides(Map.of("../../secret", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid item visibility key");
    }
}
