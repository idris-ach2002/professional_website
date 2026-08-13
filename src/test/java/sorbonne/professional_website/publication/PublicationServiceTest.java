package sorbonne.professional_website.publication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sorbonne.professional_website.audit.PublicationAuditService;
import sorbonne.professional_website.dto.response.PortfolioHealthReportResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.events.OutboxService;
import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.jobs.BackgroundJobService;
import sorbonne.professional_website.jobs.BackgroundJobType;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.service.WebsiteVersionCloner;
import sorbonne.professional_website.service.WebsiteVersionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {
    @Mock OwnerRepository owners;
    @Mock WebsiteVersionRepository versions;
    @Mock WebsiteVersionService versionService;
    @Mock BackgroundJobService jobs;
    @Mock OutboxService outbox;
    @Mock WebsiteVersionCloner cloner;
    @Mock PublicationAuditService audit;
    private PublicationService service;

    @BeforeEach void setUp(){ service = new PublicationService(owners, versions, versionService, jobs, outbox, cloner, audit); }

    @Test void schedulePersistsStateCreatesJobAndOutboxEvent(){
        Owner owner = owner(); WebsiteVersion version = version(owner, 7L, 3L);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionService.validateBeforePublish(1L,7L)).thenReturn(healthy(1L,7L));
        LocalDateTime future = LocalDateTime.now().plusHours(2);

        var result = service.schedule(1L,7L,3L,future);

        assertThat(result.publicationStatus()).isEqualTo(PublicationStatus.SCHEDULED);
        assertThat(result.scheduledAt()).isEqualTo(sorbonne.professional_website.time.PlatformTime.asUtcOffset(future));
        assertThat(result.contentRevision()).isEqualTo(4L);
        verify(jobs).create(eq(1L),eq(7L),eq(BackgroundJobType.PUBLICATION),eq(future),anyString());
        verify(outbox).record(anyString(),eq(1L),eq("WebsiteVersion"),eq(7L),eq("VERSION_PUBLICATION_SCHEDULED"),any());
    }

    @Test void staleRevisionCannotSchedule(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,4L);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));
        assertThatThrownBy(() -> service.schedule(1L,7L,3L,LocalDateTime.now().plusHours(1)))
                .isInstanceOf(PreconditionFailedException.class);
        verifyNoInteractions(jobs);
    }

    @Test void immediatePublishSupersedesPreviousAndEmitsOneOutboxEvent(){
        Owner owner=owner(); WebsiteVersion target=version(owner,7L,3L);
        WebsiteVersion previous=WebsiteVersion.builder().id(5L).owner(owner).versionTag("v1").label("old").active(true).published(true).publicationStatus(PublicationStatus.PUBLISHED).build();
        when(outbox.exists("publish:1:7:req-42")).thenReturn(false);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(target));
        when(versions.findByOwnerOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(target,previous));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionService.validateBeforePublish(1L,7L)).thenReturn(healthy(1L,7L));

        var result=service.publishNow(1L,7L,3L,"req-42");

        assertThat(result.publicationStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(result.active()).isTrue();
        assertThat(previous.getPublicationStatus()).isEqualTo(PublicationStatus.SUPERSEDED);
        verify(versions).deactivateOthersByOwnerId(1L,7L);
        verify(outbox).record(eq("publish:1:7:req-42"),eq(1L),eq("WebsiteVersion"),eq(7L),eq("WEBSITE_VERSION_PUBLISHED"),any());
    }

    @Test void repeatedIdempotencyKeyDoesNotPublishAgainOrRequireOldRevision(){
        Owner owner=owner(); WebsiteVersion already=version(owner,7L,4L); already.setPublicationStatus(PublicationStatus.PUBLISHED); already.setActive(true); already.setPublished(true);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(outbox.exists("publish:1:7:req-42")).thenReturn(true);
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(already));

        var replay=service.publishNow(1L,7L,3L,"req-42");

        assertThat(replay.contentRevision()).isEqualTo(4L);
        verify(owners).lockByOwnerId(1L);
        verify(versionService,never()).validateBeforePublish(anyLong(),anyLong());
        verify(versions,never()).deactivateOthersByOwnerId(anyLong(),anyLong());
    }



    @Test void immediatePublishCancelsExistingScheduleBeforePublishing(){
        Owner owner=owner(); WebsiteVersion target=version(owner,7L,3L);
        target.setPublicationStatus(PublicationStatus.SCHEDULED);
        target.setScheduledAt(LocalDateTime.now().plusHours(1));
        when(outbox.exists("publish:1:7:req-now")).thenReturn(false);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(target));
        when(versions.findByOwnerOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(target));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionService.validateBeforePublish(1L,7L)).thenReturn(healthy(1L,7L));

        var result=service.publishNow(1L,7L,3L,"req-now");

        assertThat(result.publicationStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        verify(jobs).cancelPublicationJobs(1L,7L);
    }

    @Test void publishedVersionCannotBeScheduledAgain(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,3L);
        version.setPublicationStatus(PublicationStatus.PUBLISHED); version.setActive(true); version.setPublished(true);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.schedule(1L,7L,3L,LocalDateTime.now().plusHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
        verifyNoInteractions(jobs);
    }

    @Test void oversizedIdempotencyKeyIsRejectedBeforeDatabaseWork(){
        assertThatThrownBy(() -> service.publishNow(1L,7L,3L,"x".repeat(97)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("96");
        verifyNoInteractions(owners, versions, versionService, jobs, outbox, audit);
    }

    @Test void terminalScheduledPublicationFailureMovesVersionToFailedAndAudits(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,3L);
        version.setPublicationStatus(PublicationStatus.SCHEDULED);
        version.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markScheduledPublicationFailed(1L,7L,"job-corr",new IllegalStateException("database unavailable"));

        assertThat(version.getPublicationStatus()).isEqualTo(PublicationStatus.FAILED);
        assertThat(version.getPublicationError()).contains("database unavailable");
        assertThat(version.getScheduledAt()).isNull();
        assertThat(version.getContentRevision()).isEqualTo(4L);
        verify(outbox).record(startsWith("publication-failed:7:4"),eq(1L),eq("WebsiteVersion"),eq(7L),eq("VERSION_PUBLICATION_FAILED"),any());
        verify(audit).record(eq(1L),eq(7L),eq("VERSION_PUBLICATION_FAILED"),eq("job-corr"),any(),any(),any());
    }


    @Test void failedScheduledPublicationCanBeRetriedByItsJob(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,4L);
        version.setPublicationStatus(PublicationStatus.FAILED);
        version.setPublicationError("transient database failure");
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));
        when(versions.findByOwnerOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(version));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionService.validateBeforePublish(1L,7L)).thenReturn(healthy(1L,7L));

        boolean published=service.publishScheduled(1L,7L);

        assertThat(published).isTrue();
        assertThat(version.getPublicationStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(version.getPublicationError()).isNull();
        verify(audit).record(eq(1L),eq(7L),eq("VERSION_PUBLISHED_RETRY"),startsWith("retry:7:"),any(),any(),any());
    }

    @Test void stalePublicationJobDoesNotPublishEditedDraft(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,5L);
        version.setPublicationStatus(PublicationStatus.DRAFT);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));

        boolean published=service.publishScheduled(1L,7L);

        assertThat(published).isFalse();
        verify(versionService,never()).validateBeforePublish(anyLong(),anyLong());
        verify(versions,never()).deactivateOthersByOwnerId(anyLong(),anyLong());
    }

    @Test void autosaveDraftMetadataUsesRevisionAndInvalidatesReadyState(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,3L); version.setPublicationStatus(PublicationStatus.READY);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result=service.autosaveDraftMetadata(1L,7L,3L,new PublicationDraftMetadataRequest("Draft updated","new description"));

        assertThat(result.label()).isEqualTo("Draft updated");
        assertThat(result.description()).isEqualTo("new description");
        assertThat(result.publicationStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(result.contentRevision()).isEqualTo(4L);
        verify(outbox).record(startsWith("draft-autosave:7:4"),eq(1L),eq("WebsiteVersion"),eq(7L),eq("VERSION_DRAFT_AUTOSAVED"),any());
        verify(audit).record(eq(1L),eq(7L),eq("VERSION_DRAFT_AUTOSAVED"),anyString(),any(),any(),any());
    }

    @Test void scheduledVersionMustBeCancelledBeforeAutosave(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,3L); version.setPublicationStatus(PublicationStatus.SCHEDULED);
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.autosaveDraftMetadata(1L,7L,3L,new PublicationDraftMetadataRequest("Draft","x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cancel");
        verify(versions,never()).saveAndFlush(any());
    }

    @Test void cancellingScheduleReturnsVersionToDraftAndCancelsQueuedJob(){
        Owner owner=owner(); WebsiteVersion version=version(owner,7L,3L); version.setPublicationStatus(PublicationStatus.SCHEDULED); version.setScheduledAt(LocalDateTime.now().plusHours(1));
        when(owners.lockByOwnerId(1L)).thenReturn(Optional.of(owner));
        when(versions.findByIdAndOwnerOwnerId(7L,1L)).thenReturn(Optional.of(version));
        when(versions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result=service.cancelSchedule(1L,7L,3L);

        assertThat(result.publicationStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(result.scheduledAt()).isNull();
        verify(jobs).cancelPublicationJobs(1L,7L);
    }

    private static Owner owner(){return Owner.builder().ownerId(1L).name("ACHABOU").firstName("Idris").age(24).address("Paris").build();}
    private static WebsiteVersion version(Owner owner,Long id,long revision){return WebsiteVersion.builder().id(id).owner(owner).contentRevision(revision).versionTag("v2").label("Draft").active(false).published(false).publicationStatus(PublicationStatus.DRAFT).build();}
    private static PortfolioHealthReportResponseDTO healthy(Long ownerId,Long versionId){return new PortfolioHealthReportResponseDTO(100,true,0,0,0,List.of(),LocalDateTime.now(),ownerId,versionId);}
}
