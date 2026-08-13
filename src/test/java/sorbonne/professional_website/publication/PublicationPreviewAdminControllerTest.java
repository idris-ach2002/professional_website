package sorbonne.professional_website.publication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PublicationPreviewAdminControllerTest {
    private PublicationPreviewService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PublicationPreviewService.class);
        mockMvc = standaloneSetup(new PublicationPreviewAdminController(service)).build();
    }

    @Test
    void previewIsNoStoreNoIndexAndUsesRequestedLocale() throws Exception {
        when(service.preview(7L, 42L, "en")).thenReturn(new OwnerResponseDTO(
                7L, 2L, "ACHABOU", "Idris", 24, true, null,
                List.of(), null, null, List.of(), List.of(), "en", List.of()
        ));

        mockMvc.perform(get("/manager/7/versions/42/preview?locale=en"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"))
                .andExpect(jsonPath("$.ownerId").value(7))
                .andExpect(jsonPath("$.locale").value("en"));

        verify(service).preview(7L, 42L, "en");
    }
}
