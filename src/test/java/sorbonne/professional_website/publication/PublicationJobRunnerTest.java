package sorbonne.professional_website.publication;

import org.junit.jupiter.api.Test;
import sorbonne.professional_website.jobs.BackgroundJob;
import sorbonne.professional_website.jobs.BackgroundJobResponse;
import sorbonne.professional_website.jobs.BackgroundJobService;
import sorbonne.professional_website.jobs.BackgroundJobStatus;
import sorbonne.professional_website.jobs.BackgroundJobType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicationJobRunnerTest {

    @Test
    void terminalExecutionFailureMovesScheduledVersionToFailed() {
        BackgroundJobService jobs = mock(BackgroundJobService.class);
        PublicationService publication = mock(PublicationService.class);
        BackgroundJob running = BackgroundJob.builder()
                .id("job-1").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION)
                .status(BackgroundJobStatus.RUNNING).attempts(3).maxAttempts(3).correlationId("corr-1")
                .build();
        when(jobs.recoverStaleRunningJobs(any())).thenReturn(List.of());
        when(jobs.duePublicationJobs(any())).thenReturn(List.of(running));
        when(jobs.claimForExecution("job-1")).thenReturn(Optional.of(running));
        doThrow(new IllegalStateException("database unavailable"))
                .when(publication).publishScheduled(1L, 7L);
        when(jobs.markFailedOrRetry(eq("job-1"), any())).thenReturn(BackgroundJobStatus.FAILED);

        new PublicationJobRunner(jobs, publication).executeDuePublications();

        verify(publication).markScheduledPublicationFailed(eq(1L), eq(7L), eq("corr-1"), any(IllegalStateException.class));
    }

    @Test
    void staleClaimedJobIsCancelledInsteadOfReportedAsSuccess() {
        BackgroundJobService jobs = mock(BackgroundJobService.class);
        PublicationService publication = mock(PublicationService.class);
        BackgroundJob running = BackgroundJob.builder()
                .id("job-stale").ownerId(1L).versionId(7L).type(BackgroundJobType.PUBLICATION)
                .status(BackgroundJobStatus.RUNNING).attempts(1).maxAttempts(3).correlationId("corr-stale")
                .build();
        when(jobs.recoverStaleRunningJobs(any())).thenReturn(List.of());
        when(jobs.duePublicationJobs(any())).thenReturn(List.of(running));
        when(jobs.claimForExecution("job-stale")).thenReturn(Optional.of(running));
        when(publication.publishScheduled(1L, 7L)).thenReturn(false);

        new PublicationJobRunner(jobs, publication).executeDuePublications();

        verify(jobs).markCancelledAfterClaim(eq("job-stale"), any(String.class));
        verify(jobs, org.mockito.Mockito.never()).markSucceeded("job-stale");
    }

    @Test
    void crashRecoveredTerminalJobAlsoMovesVersionToFailed() {
        BackgroundJobService jobs = mock(BackgroundJobService.class);
        PublicationService publication = mock(PublicationService.class);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BackgroundJobResponse recovered = new BackgroundJobResponse(
                "job-2", 1L, 7L, BackgroundJobType.PUBLICATION, BackgroundJobStatus.FAILED,
                30, 100, 3, 3, now, now.minusMinutes(5),
                now, now, "Recovered after interrupted background execution.",
                "corr-2", now.minusHours(1), now
        );
        when(jobs.recoverStaleRunningJobs(any())).thenReturn(List.of(recovered));
        when(jobs.duePublicationJobs(any())).thenReturn(List.of());

        new PublicationJobRunner(jobs, publication).executeDuePublications();

        verify(publication).markScheduledPublicationFailed(eq(1L), eq(7L), eq("corr-2"), any(IllegalStateException.class));
    }
}
