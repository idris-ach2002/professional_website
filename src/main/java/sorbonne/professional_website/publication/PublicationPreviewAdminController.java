package sorbonne.professional_website.publication;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;

@RestController
@RequestMapping("/manager/{ownerId}/versions/{versionId}/preview")
public class PublicationPreviewAdminController {
    private final PublicationPreviewService service;

    public PublicationPreviewAdminController(PublicationPreviewService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<OwnerResponseDTO> preview(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .body(service.preview(ownerId, versionId, locale));
    }
}
