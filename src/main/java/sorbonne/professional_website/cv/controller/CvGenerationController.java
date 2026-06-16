package sorbonne.professional_website.cv.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.cv.dto.CvGenerationRequest;
import sorbonne.professional_website.cv.dto.CvGenerationResponse;
import sorbonne.professional_website.cv.dto.CvSourceResponse;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.cv.service.CvGenerationService;

@RestController
@RequestMapping("/manager/{ownerId}/versions/{versionId}/cv")
public class CvGenerationController {

    private final CvGenerationService cvGenerationService;

    public CvGenerationController(CvGenerationService cvGenerationService) {
        this.cvGenerationService = cvGenerationService;
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

    @GetMapping("/version")
    public ResponseEntity<WebsiteVersionResponseDTO> readVersion(
            @PathVariable Long ownerId,
            @PathVariable Long versionId
    ) {
        return ResponseEntity.ok(cvGenerationService.readVersion(ownerId, versionId));
    }
}
