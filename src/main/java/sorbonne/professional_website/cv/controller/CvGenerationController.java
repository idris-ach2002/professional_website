package sorbonne.professional_website.cv.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.cv.dto.CvCompileJobResponse;
import sorbonne.professional_website.cv.dto.CvCompileJobStatusResponse;
import sorbonne.professional_website.cv.dto.CvExportZipResponse;
import sorbonne.professional_website.cv.dto.CvGenerationRequest;
import sorbonne.professional_website.cv.dto.CvGenerationResponse;
import sorbonne.professional_website.cv.dto.CvQualityReportResponse;
import sorbonne.professional_website.cv.dto.CvSourceResponse;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.cv.service.CvCompileJobService;
import sorbonne.professional_website.cv.service.CvGenerationService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/manager/{ownerId}/versions/{versionId}/cv")
public class CvGenerationController {

    private final CvGenerationService cvGenerationService;
    private final CvCompileJobService cvCompileJobService;

    public CvGenerationController(CvGenerationService cvGenerationService, CvCompileJobService cvCompileJobService) {
        this.cvGenerationService = cvGenerationService;
        this.cvCompileJobService = cvCompileJobService;
    }

    @GetMapping("/source")
    public ResponseEntity<CvSourceResponse> generateDefaultSource(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(cvGenerationService.generateSource(ownerId, versionId, null));
    }

    @PostMapping("/source")
    public ResponseEntity<CvSourceResponse> generateSource(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid CvGenerationRequest request
    ) {
        return ResponseEntity.ok(cvGenerationService.generateSource(ownerId, versionId, request));
    }

    @PostMapping("/preview")
    public ResponseEntity<CvGenerationResponse> preview(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid CvGenerationRequest request
    ) {
        return ResponseEntity.ok(cvGenerationService.preview(ownerId, versionId, request));
    }

    @PostMapping("/save")
    public ResponseEntity<CvGenerationResponse> save(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid CvGenerationRequest request
    ) {
        return ResponseEntity.ok(cvGenerationService.save(ownerId, versionId, request));
    }


    @PostMapping("/export-zip")
    public ResponseEntity<CvExportZipResponse> exportZip(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid CvGenerationRequest request
    ) {
        return ResponseEntity.ok(cvGenerationService.exportZip(ownerId, versionId, request));
    }

    @PostMapping("/quality")
    public ResponseEntity<CvQualityReportResponse> quality(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid CvGenerationRequest request
    ) {
        return ResponseEntity.ok(cvGenerationService.quality(ownerId, versionId, request));
    }

    @PostMapping("/compile-jobs")
    public ResponseEntity<CvCompileJobResponse> startCompileJob(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid CvGenerationRequest request
    ) {
        return ResponseEntity.ok(cvCompileJobService.startPreviewJob(ownerId, versionId, request));
    }

    @GetMapping("/compile-jobs/{jobId}")
    public ResponseEntity<CvCompileJobStatusResponse> readCompileJob(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(cvCompileJobService.readJob(jobId));
    }

    @GetMapping(value = "/compile-jobs/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompileJob(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @PathVariable String jobId
    ) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 60; i++) {
                    CvCompileJobStatusResponse status = cvCompileJobService.readJob(jobId);
                    emitter.send(SseEmitter.event().name("cv-compile").data(status));
                    if ("SUCCESS".equals(status.status()) || "FAILED".equals(status.status()) || "NOT_FOUND".equals(status.status())) {
                        emitter.complete();
                        return;
                    }
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (IOException | InterruptedException exception) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/version")
    public ResponseEntity<WebsiteVersionResponseDTO> readVersion(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(cvGenerationService.readVersion(ownerId, versionId));
    }
}
