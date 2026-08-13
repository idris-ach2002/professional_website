package sorbonne.professional_website.publication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.audit.PublicationAuditService;
import sorbonne.professional_website.dto.response.PortfolioHealthReportResponseDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.events.OutboxService;
import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.jobs.BackgroundJobService;
import sorbonne.professional_website.jobs.BackgroundJobType;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.service.WebsiteVersionService;
import sorbonne.professional_website.service.WebsiteVersionCloner;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PublicationService {
    private final OwnerRepository ownerRepository;
    private final WebsiteVersionRepository versionRepository;
    private final WebsiteVersionService versionService;
    private final BackgroundJobService jobService;
    private final OutboxService outboxService;
    private final WebsiteVersionCloner versionCloner;
    private final PublicationAuditService auditService;

    public PublicationService(OwnerRepository ownerRepository, WebsiteVersionRepository versionRepository, WebsiteVersionService versionService,
                              BackgroundJobService jobService, OutboxService outboxService, WebsiteVersionCloner versionCloner,
                              PublicationAuditService auditService) {
        this.ownerRepository = ownerRepository; this.versionRepository = versionRepository; this.versionService = versionService;
        this.jobService = jobService; this.outboxService = outboxService; this.versionCloner = versionCloner; this.auditService = auditService;
    }

    @Transactional
    public WebsiteVersionResponseDTO autosaveDraftMetadata(Long ownerId, Long versionId, long expectedRevision, PublicationDraftMetadataRequest request) {
        lockOwner(ownerId);
        WebsiteVersion version = find(ownerId, versionId);
        requireRevision(version, expectedRevision);
        if (version.getPublicationStatus() == PublicationStatus.SCHEDULED) {
            throw new IllegalStateException("Cancel the scheduled publication before editing draft metadata.");
        }
        if (version.getPublicationStatus() == PublicationStatus.PUBLISHED || version.getPublicationStatus() == PublicationStatus.SUPERSEDED) {
            throw new IllegalStateException("Published history is immutable. Clone or create a draft before editing.");
        }
        var before = WebsiteVersionMapper.toResponse(version);
        version.setLabel(request.label().trim());
        version.setDescription(request.description());
        version.setPublicationStatus(PublicationStatus.DRAFT);
        version.setPublished(false);
        version.setActive(false);
        version.setPublicationError(null);
        version.bumpContentRevision();
        WebsiteVersion savedEntity = versionRepository.saveAndFlush(version);
        var saved = WebsiteVersionMapper.toResponse(savedEntity);
        String correlationId = "draft-autosave:" + versionId + ":" + saved.contentRevision();
        outboxService.record(correlationId, ownerId, "WebsiteVersion", versionId, "VERSION_DRAFT_AUTOSAVED",
                Map.of("versionId", versionId, "contentRevision", saved.contentRevision()));
        auditService.record(ownerId, versionId, "VERSION_DRAFT_AUTOSAVED", correlationId, before, saved, Map.of());
        return saved;
    }

    @Transactional
    public WebsiteVersionResponseDTO markReady(Long ownerId, Long versionId, long expectedRevision) {
        lockOwner(ownerId);
        WebsiteVersion version = find(ownerId, versionId); requireRevision(version, expectedRevision);
        requireDraftLifecycle(version, "mark ready");
        requireHealthy(ownerId, versionId);
        var before = WebsiteVersionMapper.toResponse(version);
        version.setPublicationStatus(PublicationStatus.READY); version.setPublished(false); version.setActive(false); version.setPublicationError(null); version.bumpContentRevision();
        String correlationId = "version-ready:"+versionId+":"+version.getContentRevision();
        outboxService.record(correlationId, ownerId, "WebsiteVersion", versionId, "VERSION_READY", Map.of("versionId", versionId));
        var saved = WebsiteVersionMapper.toResponse(versionRepository.saveAndFlush(version));
        auditService.record(ownerId, versionId, "VERSION_READY", correlationId, before, saved, Map.of());
        return saved;
    }

    @Transactional
    public WebsiteVersionResponseDTO publishNow(Long ownerId, Long versionId, long expectedRevision, String idempotencyKey) {
        String eventKey = publicationEventKey(ownerId, versionId, expectedRevision, idempotencyKey);
        lockOwner(ownerId);
        // The idempotency check must happen after the owner lock. Otherwise two concurrent
        // requests can both observe a missing key before one of them commits.
        if (outboxService.exists(eventKey)) {
            return WebsiteVersionMapper.toResponse(find(ownerId, versionId));
        }
        WebsiteVersion version = find(ownerId, versionId); requireRevision(version, expectedRevision);
        requirePublishableLifecycle(version);
        requireHealthy(ownerId, versionId);
        if (version.getPublicationStatus() == PublicationStatus.SCHEDULED) {
            jobService.cancelPublicationJobs(ownerId, versionId);
        }
        var before = WebsiteVersionMapper.toResponse(version);
        var published = publishLocked(ownerId, version, eventKey);
        auditService.record(ownerId, versionId, "VERSION_PUBLISHED", eventKey, before, published, Map.of("idempotencyKey", eventKey));
        return published;
    }

    @Transactional
    public WebsiteVersionResponseDTO schedule(Long ownerId, Long versionId, long expectedRevision, LocalDateTime publishAt) {
        if (publishAt == null || !publishAt.isAfter(sorbonne.professional_website.time.PlatformTime.utcNow())) throw new IllegalArgumentException("publishAt must be in the future.");
        lockOwner(ownerId); WebsiteVersion version=find(ownerId, versionId); requireRevision(version, expectedRevision);
        requireSchedulableLifecycle(version);
        requireHealthy(ownerId, versionId);
        var before = WebsiteVersionMapper.toResponse(version);
        version.setActive(false); version.setPublished(false); version.setPublicationStatus(PublicationStatus.SCHEDULED); version.setScheduledAt(publishAt); version.setPublicationError(null); version.bumpContentRevision();
        WebsiteVersion saved=versionRepository.saveAndFlush(version);
        jobService.cancelPublicationJobs(ownerId, versionId);
        String correlationId = "publication:"+versionId+":"+saved.getContentRevision();
        jobService.create(ownerId, versionId, BackgroundJobType.PUBLICATION, publishAt, correlationId);
        outboxService.record("version-scheduled:"+versionId+":"+saved.getContentRevision(), ownerId, "WebsiteVersion", versionId, "VERSION_PUBLICATION_SCHEDULED", Map.of("versionId",versionId,"publishAt",publishAt.toString()));
        var response = WebsiteVersionMapper.toResponse(saved);
        auditService.record(ownerId, versionId, "VERSION_PUBLICATION_SCHEDULED", correlationId, before, response, Map.of("publishAt", publishAt.toString()));
        return response;
    }

    @Transactional
    public WebsiteVersionResponseDTO cancelSchedule(Long ownerId, Long versionId, long expectedRevision) {
        lockOwner(ownerId); WebsiteVersion version=find(ownerId,versionId); requireRevision(version,expectedRevision);
        if (version.getPublicationStatus()!=PublicationStatus.SCHEDULED) throw new IllegalStateException("Version is not scheduled.");
        var before = WebsiteVersionMapper.toResponse(version);
        version.setPublicationStatus(PublicationStatus.DRAFT); version.setScheduledAt(null); version.setPublished(false); version.setActive(false); version.bumpContentRevision();
        jobService.cancelPublicationJobs(ownerId, versionId);
        WebsiteVersion saved=versionRepository.saveAndFlush(version);
        String correlationId = "version-schedule-cancelled:"+versionId+":"+saved.getContentRevision();
        outboxService.record(correlationId, ownerId, "WebsiteVersion", versionId, "VERSION_PUBLICATION_CANCELLED", Map.of("versionId",versionId));
        var response = WebsiteVersionMapper.toResponse(saved);
        auditService.record(ownerId, versionId, "VERSION_PUBLICATION_CANCELLED", correlationId, before, response, Map.of());
        return response;
    }

    @Transactional
    public boolean publishScheduled(Long ownerId, Long versionId) {
        lockOwner(ownerId);
        WebsiteVersion version=find(ownerId,versionId);
        PublicationStatus status = version.getPublicationStatus();
        boolean scheduledDue = status == PublicationStatus.SCHEDULED
                && version.getScheduledAt() != null
                && !version.getScheduledAt().isAfter(sorbonne.professional_website.time.PlatformTime.utcNow());
        boolean manualRetry = status == PublicationStatus.FAILED;
        if (!scheduledDue && !manualRetry) return false;

        requireHealthy(ownerId, versionId);
        var before = WebsiteVersionMapper.toResponse(version);
        String correlationId = (manualRetry ? "retry:" : "scheduled:") + versionId + ":" + version.getContentRevision();
        var published = publishLocked(ownerId, version, correlationId);
        auditService.record(ownerId, versionId, manualRetry ? "VERSION_PUBLISHED_RETRY" : "VERSION_PUBLISHED_SCHEDULED",
                correlationId, before, published, Map.of());
        return true;
    }

    @Transactional
    public void markScheduledPublicationFailed(Long ownerId, Long versionId, String correlationId, Throwable failure) {
        lockOwner(ownerId);
        WebsiteVersion version = find(ownerId, versionId);
        if (version.getPublicationStatus() != PublicationStatus.SCHEDULED
                && version.getPublicationStatus() != PublicationStatus.PUBLISHING) {
            return;
        }
        var before = WebsiteVersionMapper.toResponse(version);
        String safeError = safeFailureMessage(failure);
        version.setPublicationStatus(PublicationStatus.FAILED);
        version.setPublicationError(safeError);
        version.setScheduledAt(null);
        version.setPublished(false);
        version.setActive(false);
        version.bumpContentRevision();
        WebsiteVersion savedEntity = versionRepository.saveAndFlush(version);
        var saved = WebsiteVersionMapper.toResponse(savedEntity);
        String eventKey = "publication-failed:" + versionId + ":" + saved.contentRevision();
        outboxService.record(eventKey, ownerId, "WebsiteVersion", versionId, "VERSION_PUBLICATION_FAILED",
                Map.of("versionId", versionId, "error", safeError));
        auditService.record(ownerId, versionId, "VERSION_PUBLICATION_FAILED",
                correlationId == null || correlationId.isBlank() ? eventKey : correlationId, before, saved, Map.of("error", safeError));
    }

    private WebsiteVersionResponseDTO publishLocked(Long ownerId, WebsiteVersion version, String eventKey) {
        versionRepository.deactivateOthersByOwnerId(ownerId, version.getId());
        for (WebsiteVersion previous : versionRepository.findByOwnerOwnerIdOrderByCreatedAtDesc(ownerId)) {
            if (!previous.getId().equals(version.getId()) && previous.getPublicationStatus()==PublicationStatus.PUBLISHED) {
                previous.setPublicationStatus(PublicationStatus.SUPERSEDED); previous.setActive(false);
            }
        }
        version.setPublicationStatus(PublicationStatus.PUBLISHING); versionRepository.saveAndFlush(version);
        version.setActive(true); version.setPublished(true); version.setScheduledAt(null); version.setPublishedAt(sorbonne.professional_website.time.PlatformTime.utcNow()); version.setPublicationError(null); version.setPublicationStatus(PublicationStatus.PUBLISHED); version.bumpContentRevision();
        WebsiteVersion saved=versionRepository.saveAndFlush(version);
        outboxService.record(eventKey, ownerId, "WebsiteVersion", version.getId(), "WEBSITE_VERSION_PUBLISHED", Map.of("versionId",version.getId(),"contentRevision",saved.getContentRevision()));
        return WebsiteVersionMapper.toResponse(saved);
    }

    @Transactional
    public WebsiteVersionResponseDTO rollbackTo(Long ownerId, Long sourceVersionId, long expectedSourceRevision) {
        var owner = ownerRepository.lockByOwnerId(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner"));
        WebsiteVersion source = find(ownerId, sourceVersionId); requireRevision(source, expectedSourceRevision);
        var sourceSnapshot = WebsiteVersionMapper.toResponse(source);
        String suffix = String.valueOf(System.currentTimeMillis());
        WebsiteVersion rollback = WebsiteVersion.builder()
                .versionTag(("rollback-" + source.getVersionTag() + "-" + suffix).substring(0, Math.min(80, ("rollback-" + source.getVersionTag() + "-" + suffix).length())))
                .label("Rollback — " + source.getLabel())
                .description("Snapshot restauré depuis la version " + source.getVersionTag())
                .active(false).published(false).publicationStatus(PublicationStatus.DRAFT).owner(owner).build();
        rollback.attachProfile(versionCloner.copyProfile(source.getProfile()));
        rollback.attachTimeline(versionCloner.copyTimeline(source.getTimeline()));
        rollback.clearAndAttachProjects(versionCloner.copyProjects(source.getProjects()));
        rollback = versionRepository.saveAndFlush(rollback);
        requireHealthy(ownerId, rollback.getId());
        outboxService.record("version-rollback-created:"+rollback.getId(), ownerId, "WebsiteVersion", rollback.getId(), "VERSION_ROLLBACK_CREATED",
                Map.of("sourceVersionId", sourceVersionId, "rollbackVersionId", rollback.getId()));
        WebsiteVersionResponseDTO published = publishLocked(ownerId, rollback, "rollback-published:"+rollback.getId());
        String rollbackCorrelationId = "version-rollback:"+rollback.getId();
        outboxService.record(rollbackCorrelationId, ownerId, "WebsiteVersion", rollback.getId(), "WEBSITE_VERSION_ROLLED_BACK",
                Map.of("sourceVersionId", sourceVersionId, "rollbackVersionId", rollback.getId()));
        auditService.record(ownerId, rollback.getId(), "WEBSITE_VERSION_ROLLED_BACK", rollbackCorrelationId, sourceSnapshot, published,
                Map.of("sourceVersionId", sourceVersionId, "rollbackVersionId", rollback.getId()));
        return published;
    }

    private String publicationEventKey(Long ownerId, Long versionId, long expectedRevision, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return "publish:" + ownerId + ":" + versionId + ":revision:" + expectedRevision;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 96) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 96 characters.");
        }
        return "publish:" + ownerId + ":" + versionId + ":" + normalized;
    }

    private void requireDraftLifecycle(WebsiteVersion version, String action) {
        PublicationStatus status = version.getPublicationStatus();
        if (status != PublicationStatus.DRAFT && status != PublicationStatus.READY && status != PublicationStatus.FAILED) {
            throw new IllegalStateException("Cannot " + action + " a version in " + status + " state.");
        }
    }

    private void requirePublishableLifecycle(WebsiteVersion version) {
        PublicationStatus status = version.getPublicationStatus();
        if (status != PublicationStatus.DRAFT && status != PublicationStatus.READY
                && status != PublicationStatus.FAILED && status != PublicationStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot publish a version in " + status + " state.");
        }
    }

    private void requireSchedulableLifecycle(WebsiteVersion version) {
        PublicationStatus status = version.getPublicationStatus();
        if (status != PublicationStatus.DRAFT && status != PublicationStatus.READY
                && status != PublicationStatus.FAILED && status != PublicationStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot schedule a version in " + status + " state.");
        }
    }

    private static String safeFailureMessage(Throwable failure) {
        String message = failure == null ? "Unknown publication failure" : failure.getMessage();
        if (message == null || message.isBlank()) message = failure == null ? "Unknown publication failure" : failure.getClass().getSimpleName();
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private void requireHealthy(Long ownerId, Long versionId) {
        PortfolioHealthReportResponseDTO report=versionService.validateBeforePublish(ownerId,versionId);
        if (!report.publishable()) throw new IllegalStateException("Publication validation failed.");
    }
    private void lockOwner(Long ownerId){ ownerRepository.lockByOwnerId(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner not found: "+ownerId)); }
    private WebsiteVersion find(Long ownerId, Long versionId){ return versionRepository.findByIdAndOwnerOwnerId(versionId,ownerId).orElseThrow(() -> new ResourceNotFoundException("Website version not found: "+versionId)); }
    private void requireRevision(WebsiteVersion version,long expected){ if(version.getContentRevision()!=expected) throw new PreconditionFailedException("Version changed since last read. Refresh before publishing."); }
}
