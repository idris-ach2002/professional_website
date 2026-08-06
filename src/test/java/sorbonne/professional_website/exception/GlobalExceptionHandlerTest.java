package sorbonne.professional_website.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/test");
        request.setAttribute(RequestIdFilter.ATTRIBUTE_NAME, "request-test-123");
    }

    @Test
    void notFoundErrorsUseTheCommonContract() {
        var response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Project"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().requestId()).isEqualTo("request-test-123");
    }

    @Test
    void unexpectedErrorsNeverExposeTheirTechnicalMessage() {
        var response = handler.handleUnexpectedException(
                new RuntimeException("database password should stay private"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Une erreur interne est survenue.");
        assertThat(response.getBody().message()).doesNotContain("password");
    }
}
