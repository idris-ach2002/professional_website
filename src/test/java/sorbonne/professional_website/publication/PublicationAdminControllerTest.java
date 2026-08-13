package sorbonne.professional_website.publication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.exception.GlobalExceptionHandler;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PublicationAdminControllerTest {
    private PublicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PublicationService.class);
        mockMvc = standaloneSetup(new PublicationAdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publishRequiresIfMatchBeforeCallingService() throws Exception {
        mockMvc.perform(put("/manager/7/versions/42/publication/publish")
                        .header("Idempotency-Key", "intent-1"))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));

        verify(service, never()).publishNow(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void publishForwardsIdempotencyKeyAndReturnsNextEntityTag() throws Exception {
        when(service.publishNow(7L, 42L, 9L, "intent-1")).thenReturn(version(42L, 10L, PublicationStatus.PUBLISHED));

        mockMvc.perform(put("/manager/7/versions/42/publication/publish")
                        .header(IF_MATCH, "\"version-42-9\"")
                        .header("Idempotency-Key", "intent-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"version-42-10\""))
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"));

        verify(service).publishNow(7L, 42L, 9L, "intent-1");
    }

    @Test
    void scheduleNormalizesExplicitOffsetToUtcBeforeCallingService() throws Exception {
        LocalDateTime expectedUtc = LocalDateTime.of(2026, 8, 14, 12, 30);
        when(service.schedule(7L, 42L, 9L, expectedUtc)).thenReturn(version(42L, 10L, PublicationStatus.SCHEDULED));

        mockMvc.perform(put("/manager/7/versions/42/publication/schedule")
                        .header(IF_MATCH, "\"version-42-9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishAt\":\"2026-08-14T14:30:00+02:00\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"version-42-10\""))
                .andExpect(jsonPath("$.publicationStatus").value("SCHEDULED"));

        verify(service).schedule(7L, 42L, 9L, expectedUtc);
    }

    @Test
    void draftMetadataAutosaveIsProtectedByIfMatchAndReturnsRevisionTag() throws Exception {
        when(service.autosaveDraftMetadata(eq(7L), eq(42L), eq(9L), eq(new PublicationDraftMetadataRequest("Draft", "Internal"))))
                .thenReturn(version(42L, 10L, PublicationStatus.DRAFT));

        mockMvc.perform(put("/manager/7/versions/42/publication/draft-metadata")
                        .header(IF_MATCH, "\"version-42-9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Draft\",\"description\":\"Internal\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"version-42-10\""));

        verify(service).autosaveDraftMetadata(7L, 42L, 9L, new PublicationDraftMetadataRequest("Draft", "Internal"));
    }

    private static WebsiteVersionResponseDTO version(long id, long revision, PublicationStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 10, 0);
        return new WebsiteVersionResponseDTO(
                id, revision, "v2", "Draft", null,
                status == PublicationStatus.PUBLISHED,
                status == PublicationStatus.PUBLISHED,
                status,
                status == PublicationStatus.SCHEDULED ? OffsetDateTime.parse("2026-08-14T12:30:00Z") : null,
                status == PublicationStatus.PUBLISHED ? OffsetDateTime.parse("2026-08-13T10:00:00Z") : null,
                null, now, now, null, null, List.of()
        );
    }
}
