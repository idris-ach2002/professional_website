package sorbonne.professional_website.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.events.OutboxService;
import sorbonne.professional_website.time.PlatformTime;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServiceTest {
    @Mock BackgroundJobRepository repository; @Mock OutboxService outbox;

    @Test void onlyOneRunnerCanClaimQueuedJob(){
        BackgroundJob job=BackgroundJob.builder().id("j1").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION).status(BackgroundJobStatus.QUEUED).maxAttempts(3).build();
        when(repository.lockById("j1")).thenReturn(Optional.of(job));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BackgroundJobService service=new BackgroundJobService(repository,outbox);

        var first=service.claimForExecution("j1");
        var second=service.claimForExecution("j1");

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(job.getAttempts()).isEqualTo(1);
    }

    @Test void failureUsesBackoffBeforeTerminalFailure(){
        BackgroundJob job=BackgroundJob.builder().id("j1").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION).status(BackgroundJobStatus.RUNNING).attempts(1).maxAttempts(3).build();
        when(repository.lockById("j1")).thenReturn(Optional.of(job));
        BackgroundJobService service=new BackgroundJobService(repository,outbox);
        LocalDateTime before=PlatformTime.utcNow();

        service.markFailedOrRetry("j1",new IllegalStateException("boom"));

        assertThat(job.getStatus()).isEqualTo(BackgroundJobStatus.RETRYING);
        assertThat(job.getExecuteAfter()).isAfter(before);
        assertThat(job.getLastError()).contains("boom");
    }
    @Test void recoverStaleRunningJobReturnsItToRetryQueue(){
        BackgroundJob job=BackgroundJob.builder().id("j2").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION)
                .status(BackgroundJobStatus.RUNNING).attempts(1).maxAttempts(3).heartbeatAt(PlatformTime.utcNow().minusMinutes(5)).build();
        when(repository.lockStaleRunning(eq(BackgroundJobStatus.RUNNING), any())).thenReturn(java.util.List.of(job));
        BackgroundJobService service=new BackgroundJobService(repository,outbox);

        var recovered=service.recoverStaleRunningJobs(PlatformTime.utcNow().minusMinutes(2));

        assertThat(recovered).hasSize(1);
        assertThat(recovered.getFirst().status()).isEqualTo(BackgroundJobStatus.RETRYING);
        assertThat(job.getStatus()).isEqualTo(BackgroundJobStatus.RETRYING);
        assertThat(job.getExecuteAfter()).isNotNull();
        assertThat(job.getLastError()).contains("Recovered");
    }

    @Test void manualRetryResetsAutomaticRetryBudget(){
        BackgroundJob job=BackgroundJob.builder().id("j4").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION)
                .status(BackgroundJobStatus.FAILED).attempts(3).maxAttempts(3).build();
        when(repository.lockById("j4")).thenReturn(Optional.of(job));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BackgroundJobService service=new BackgroundJobService(repository,outbox);

        var retried=service.retry(1L,"j4");

        assertThat(retried.status()).isEqualTo(BackgroundJobStatus.RETRYING);
        assertThat(retried.attempts()).isZero();
        assertThat(retried.executeAfter()).isNotNull();
    }

    @Test void terminalFailureReturnsFailedStatus(){
        BackgroundJob job=BackgroundJob.builder().id("j3").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION)
                .status(BackgroundJobStatus.RUNNING).attempts(3).maxAttempts(3).build();
        when(repository.lockById("j3")).thenReturn(Optional.of(job));
        BackgroundJobService service=new BackgroundJobService(repository,outbox);

        var status=service.markFailedOrRetry("j3",new IllegalStateException("boom"));

        assertThat(status).isEqualTo(BackgroundJobStatus.FAILED);
        assertThat(job.getCompletedAt()).isNotNull();
    }

}
