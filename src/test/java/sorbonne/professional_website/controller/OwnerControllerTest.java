package sorbonne.professional_website.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.exception.GlobalExceptionHandler;
import sorbonne.professional_website.service.OwnerService;

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

class OwnerControllerTest {

    private OwnerService ownerService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ownerService = mock(OwnerService.class);
        mockMvc = standaloneSetup(new OwnerController(ownerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOwnerExposesTheCurrentEntityTag() throws Exception {
        when(ownerService.getOwnerById(7L)).thenReturn(owner(7L, 4L));

        mockMvc.perform(get("/manager/7"))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"owner-7-4\""))
                .andExpect(jsonPath("$.rowVersion").value(4));
    }

    @Test
    void updateRequiresIfMatch() throws Exception {
        mockMvc.perform(put("/manager/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOwnerJson()))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));

        verify(ownerService, never()).updateOwner(any(), anyLong(), any(OwnerRequestDTO.class));
    }

    @Test
    void updateUsesExpectedRevisionAndExposesTheNextEntityTag() throws Exception {
        when(ownerService.updateOwner(any(), anyLong(), any(OwnerRequestDTO.class)))
                .thenReturn(owner(7L, 5L));

        mockMvc.perform(put("/manager/7")
                        .header(IF_MATCH, "\"owner-7-4\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOwnerJson()))
                .andExpect(status().isOk())
                .andExpect(header().string(ETAG, "\"owner-7-5\""))
                .andExpect(jsonPath("$.rowVersion").value(5));

        verify(ownerService).updateOwner(eq(7L), eq(4L), any(OwnerRequestDTO.class));
    }

    private static OwnerResponseDTO owner(long id, long revision) {
        return new OwnerResponseDTO(
                id,
                revision,
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
                "fr",
                List.of()
        );
    }

    private static String validOwnerJson() {
        return """
                {
                  "name": "ACHABOU",
                  "firstName": "Idris",
                  "age": 24,
                  "active": true,
                  "address": "Paris",
                  "contacts": []
                }
                """;
    }
}
