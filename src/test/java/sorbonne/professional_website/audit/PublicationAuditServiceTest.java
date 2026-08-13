package sorbonne.professional_website.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicationAuditServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedActorAndBeforeAfterSnapshots() {
        PublicationAuditRepository repository = mock(PublicationAuditRepository.class);
        when(repository.save(any(PublicationAuditEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublicationAuditService service = new PublicationAuditService(repository, new ObjectMapper().findAndRegisterModules());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("idris", "n/a", List.of())
        );

        PublicationAuditEntry entry = service.record(
                1L,
                2L,
                "VERSION_PUBLISHED",
                "corr-1",
                Map.of("status", "READY"),
                Map.of("status", "PUBLISHED"),
                Map.of("source", "manual")
        );

        assertThat(entry.getActor()).isEqualTo("idris");
        assertThat(entry.getBeforeJson()).contains("READY");
        assertThat(entry.getAfterJson()).contains("PUBLISHED");
        assertThat(entry.getMetadataJson()).contains("manual");
        verify(repository).save(entry);
    }

    @Test
    void usesSystemActorOutsideAuthenticatedRequest() {
        PublicationAuditRepository repository = mock(PublicationAuditRepository.class);
        when(repository.save(any(PublicationAuditEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublicationAuditService service = new PublicationAuditService(repository, new ObjectMapper());

        PublicationAuditEntry entry = service.record(1L, 2L, "VERSION_PUBLISHED_SCHEDULED", "corr-2", null, null, Map.of());

        assertThat(entry.getActor()).isEqualTo("SYSTEM");
    }
}
