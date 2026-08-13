package sorbonne.professional_website.audit;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager/{ownerId}/publication-audit")
public class PublicationAuditAdminController {
    private final PublicationAuditService service;

    public PublicationAuditAdminController(PublicationAuditService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PublicationAuditResponse>> list(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long versionId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.list(ownerId, versionId));
    }
}
