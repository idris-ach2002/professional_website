package sorbonne.professional_website.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.time.PlatformTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDispatchServiceTest {
    @Mock OutboxEventRepository repository;

    @Test void claimMarksEventProcessingAndIncrementsAttempt(){
        OutboxEvent event = event(OutboxStatus.PENDING, 0);
        when(repository.lockDue(eq(OutboxStatus.PENDING), any())).thenReturn(List.of(event));
        OutboxDispatchService service = new OutboxDispatchService(repository);

        var claimed = service.claimDue(PlatformTime.utcNow());

        assertThat(claimed).hasSize(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getClaimedAt()).isNotNull();
    }

    @Test void failedDispatchUsesBackoffBeforeDeadLetter(){
        OutboxEvent event = event(OutboxStatus.PROCESSING, 1);
        when(repository.lockById("e1")).thenReturn(Optional.of(event));
        OutboxDispatchService service = new OutboxDispatchService(repository);

        service.markFailed("e1", new IllegalStateException("boom"));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getNextAttemptAt()).isAfter(PlatformTime.utcNow().minusSeconds(1));
        assertThat(event.getLastError()).contains("boom");
    }

    @Test void staleProcessingIsRecoveredAfterInterruptedProcess(){
        OutboxEvent event = event(OutboxStatus.PROCESSING, 2);
        event.setClaimedAt(PlatformTime.utcNow().minusMinutes(5));
        when(repository.lockStaleProcessing(eq(OutboxStatus.PROCESSING), any())).thenReturn(List.of(event));
        OutboxDispatchService service = new OutboxDispatchService(repository);

        int recovered = service.recoverStale(PlatformTime.utcNow().minusMinutes(2));

        assertThat(recovered).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getClaimedAt()).isNull();
        assertThat(event.getLastError()).contains("Recovered");
    }

    @Test void deadEventCanBeManuallyRequeued(){
        OutboxEvent event = event(OutboxStatus.DEAD, 5);
        when(repository.lockById("e1")).thenReturn(Optional.of(event));
        OutboxDispatchService service = new OutboxDispatchService(repository);

        var response = service.retryDead(1L, "e1");

        assertThat(response.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(response.attempts()).isZero();
    }

    private static OutboxEvent event(OutboxStatus status, int attempts){
        return OutboxEvent.builder().id("e1").eventKey("k1").ownerId(1L).aggregateType("WebsiteVersion")
                .aggregateId("7").eventType("WEBSITE_VERSION_PUBLISHED").payloadJson("{}")
                .status(status).attempts(attempts).nextAttemptAt(PlatformTime.utcNow()).build();
    }
}
