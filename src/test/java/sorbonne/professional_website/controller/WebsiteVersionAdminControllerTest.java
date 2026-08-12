package sorbonne.professional_website.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.exception.GlobalExceptionHandler;
import sorbonne.professional_website.service.WebsiteVersionService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WebsiteVersionAdminControllerTest {

    private WebsiteVersionService websiteVersionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        websiteVersionService = mock(WebsiteVersionService.class);
        mockMvc = standaloneSetup(new WebsiteVersionAdminController(websiteVersionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getVersionExposesStrongEntityTagForTheCurrentContentRevision() throws Exception {
        when(websiteVersionService.getVersion(7L, 42L)).thenReturn(version(42L, 9L));

        mockMvc.perform(get("/manager/7/versions/42"))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"version-42-9\""))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.contentRevision").value(9));
    }

    @Test
    void mutationWithoutIfMatchIsRejectedBeforeTheServiceIsCalled() throws Exception {
        mockMvc.perform(put("/manager/7/versions/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Draft\"}"))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));

        verify(websiteVersionService, never()).updateVersion(any(), any(), anyLong(), any(WebsiteVersionRequestDTO.class));
    }

    @Test
    void mutationUsesIfMatchRevisionAndReturnsTheNextEntityTag() throws Exception {
        when(websiteVersionService.updateVersion(any(), any(), anyLong(), any(WebsiteVersionRequestDTO.class)))
                .thenReturn(version(42L, 10L));

        mockMvc.perform(put("/manager/7/versions/42")
                        .header(IF_MATCH, "\"version-42-9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Draft\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"version-42-10\""))
                .andExpect(jsonPath("$.contentRevision").value(10));

        verify(websiteVersionService).updateVersion(
                eq(7L),
                eq(42L),
                eq(9L),
                any(WebsiteVersionRequestDTO.class)
        );
    }

    @Test
    void entityTagFromAnotherVersionIsRejectedAsConcurrentModification() throws Exception {
        mockMvc.perform(put("/manager/7/versions/42")
                        .header(IF_MATCH, "\"version-99-9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Draft\"}"))
                .andExpect(status().is(412))
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                .andExpect(jsonPath("$.details.reloadRequired").value(true));

        verify(websiteVersionService, never()).updateVersion(any(), any(), anyLong(), any(WebsiteVersionRequestDTO.class));
    }

    private static WebsiteVersionResponseDTO version(long id, long revision) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 12, 0);
        return new WebsiteVersionResponseDTO(
                id,
                revision,
                "v1",
                "Draft",
                null,
                false,
                false,
                now,
                now,
                null,
                null,
                List.of()
        );
    }
}
