package sorbonne.professional_website.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.exception.GlobalExceptionHandler;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.service.WebsiteService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WebsiteControllerTest {

    private WebsiteService websiteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        websiteService = mock(WebsiteService.class);
        mockMvc = standaloneSetup(new WebsiteController(websiteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void defaultWebsiteIsPublicAndForwardsTheLocale() throws Exception {
        OwnerResponseDTO owner = new OwnerResponseDTO(
                1L,
                "ACHABOU",
                "Idris",
                24,
                true,
                "Paris",
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                "en",
                List.of()
        );
        when(websiteService.getFirstOwner("en")).thenReturn(owner);

        mockMvc.perform(get("/website/default").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(1))
                .andExpect(jsonPath("$.locale").value("en"));

        verify(websiteService).getFirstOwner("en");
    }

    @Test
    void missingOwnerReturnsTheStandardized404Payload() throws Exception {
        when(websiteService.getPublicWebsiteByOwnerId(999L, "fr"))
                .thenThrow(new ResourceNotFoundException("Owner"));

        mockMvc.perform(get("/website/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Owner introuvable."))
                .andExpect(jsonPath("$.path").value("/website/999"))
                .andExpect(jsonPath("$.requestId").value("unavailable"));
    }
}
