package sorbonne.professional_website.applications.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.applications.dto.ApplicationDashboardResponse;
import sorbonne.professional_website.applications.dto.CoverLetterRequest;
import sorbonne.professional_website.applications.dto.CoverLetterResponse;
import sorbonne.professional_website.applications.dto.JobApplicationRequest;
import sorbonne.professional_website.applications.dto.JobApplicationResponse;
import sorbonne.professional_website.applications.dto.OfferAnalysisRequest;
import sorbonne.professional_website.applications.dto.OfferAnalysisResponse;
import sorbonne.professional_website.applications.dto.CvVariantProposalDto;
import sorbonne.professional_website.applications.dto.LetterTemplateResponse;
import sorbonne.professional_website.applications.dto.LetterVariantProposalDto;
import sorbonne.professional_website.applications.dto.SmartApplicationPackResponse;
import sorbonne.professional_website.applications.dto.SmartOfferAnalysisResponse;
import sorbonne.professional_website.applications.entity.ApplicationStatus;
import sorbonne.professional_website.applications.service.JobApplicationService;
import sorbonne.professional_website.applications.service.SmartApplicationService;

import java.util.List;

@RestController
@RequestMapping("/manager/{ownerId}/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final SmartApplicationService smartApplicationService;

    public JobApplicationController(
            JobApplicationService jobApplicationService,
            SmartApplicationService smartApplicationService
    ) {
        this.jobApplicationService = jobApplicationService;
        this.smartApplicationService = smartApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> list(
            @PathVariable Long ownerId,
            @RequestParam(required = false) ApplicationStatus status
    ) {
        return ResponseEntity.ok(jobApplicationService.list(ownerId, status));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApplicationDashboardResponse> dashboard(@PathVariable Long ownerId) {
        return ResponseEntity.ok(jobApplicationService.dashboard(ownerId));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<JobApplicationResponse> read(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(jobApplicationService.read(ownerId, applicationId));
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(
            @PathVariable Long ownerId,
            @RequestBody @Valid JobApplicationRequest request
    ) {
        return ResponseEntity.ok(jobApplicationService.create(ownerId, request));
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<JobApplicationResponse> update(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestBody @Valid JobApplicationRequest request
    ) {
        return ResponseEntity.ok(jobApplicationService.update(ownerId, applicationId, request));
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId
    ) {
        jobApplicationService.delete(ownerId, applicationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{applicationId}/status/{status}")
    public ResponseEntity<JobApplicationResponse> markStatus(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @PathVariable ApplicationStatus status
    ) {
        return ResponseEntity.ok(jobApplicationService.markStatus(ownerId, applicationId, status));
    }

    @PostMapping("/analyze-offer")
    public ResponseEntity<OfferAnalysisResponse> analyzeOffer(
            @PathVariable Long ownerId,
            @RequestBody @Valid OfferAnalysisRequest request
    ) {
        return ResponseEntity.ok(jobApplicationService.analyzeOffer(request));
    }

    @PostMapping("/{applicationId}/cover-letter/preview")
    public ResponseEntity<CoverLetterResponse> previewCoverLetter(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestBody(required = false) @Valid CoverLetterRequest request
    ) {
        return ResponseEntity.ok(jobApplicationService.previewCoverLetter(ownerId, applicationId, request));
    }

    @PostMapping("/{applicationId}/cover-letter/save")
    public ResponseEntity<CoverLetterResponse> saveCoverLetter(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestBody(required = false) @Valid CoverLetterRequest request
    ) {
        return ResponseEntity.ok(jobApplicationService.saveCoverLetter(ownerId, applicationId, request));
    }

    @PostMapping("/{applicationId}/export-zip")
    public ResponseEntity<CoverLetterResponse> exportZip(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestBody(required = false) @Valid CoverLetterRequest request
    ) {
        return ResponseEntity.ok(jobApplicationService.exportApplicationZip(ownerId, applicationId, request));
    }


    @GetMapping("/letter-templates")
    public ResponseEntity<List<LetterTemplateResponse>> listLetterTemplates(@PathVariable Long ownerId) {
        return ResponseEntity.ok(smartApplicationService.listLetterTemplates());
    }

    @PostMapping("/{applicationId}/analyze-smart")
    public ResponseEntity<SmartOfferAnalysisResponse> analyzeSmart(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long versionId
    ) {
        return ResponseEntity.ok(smartApplicationService.analyzeSmart(ownerId, applicationId, versionId));
    }

    @PostMapping("/{applicationId}/generate-cv-variants")
    public ResponseEntity<List<CvVariantProposalDto>> generateCvVariants(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long versionId
    ) {
        return ResponseEntity.ok(smartApplicationService.generateCvVariants(ownerId, applicationId, versionId));
    }

    @PostMapping("/{applicationId}/generate-letter-variants")
    public ResponseEntity<List<LetterVariantProposalDto>> generateLetterVariants(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long versionId
    ) {
        return ResponseEntity.ok(smartApplicationService.generateLetterVariants(ownerId, applicationId, versionId));
    }

    @PostMapping("/{applicationId}/smart-pack")
    public ResponseEntity<SmartApplicationPackResponse> exportSmartPack(
            @PathVariable Long ownerId,
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long versionId
    ) {
        return ResponseEntity.ok(smartApplicationService.exportSmartPack(ownerId, applicationId, versionId));
    }

}
