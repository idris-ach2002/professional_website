package sorbonne.professional_website.publication;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.concurrency.HttpEntityTag;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;

@RestController
@RequestMapping("/manager/{ownerId}/versions/{versionId}/publication")
public class PublicationAdminController {
    private final PublicationService service;
    public PublicationAdminController(PublicationService service){this.service=service;}

    @PutMapping("/draft-metadata")
    public ResponseEntity<WebsiteVersionResponseDTO> autosaveDraftMetadata(
            @PathVariable Long ownerId,
            @PathVariable Long versionId,
            @RequestHeader(value="If-Match",required=false) String ifMatch,
            @RequestBody @Valid PublicationDraftMetadataRequest request
    ) {
        var result = service.autosaveDraftMetadata(ownerId, versionId, HttpEntityTag.requireRevision(ifMatch, "version", versionId), request);
        return ResponseEntity.ok().eTag(HttpEntityTag.version(result.id(), result.contentRevision())).body(result);
    }

    @PutMapping("/ready")
    public ResponseEntity<WebsiteVersionResponseDTO> ready(@PathVariable Long ownerId,@PathVariable Long versionId,@RequestHeader(value="If-Match",required=false) String ifMatch){
        var result=service.markReady(ownerId,versionId,HttpEntityTag.requireRevision(ifMatch,"version",versionId));
        return ResponseEntity.ok().eTag(HttpEntityTag.version(result.id(),result.contentRevision())).body(result);
    }
    @PutMapping("/publish")
    public ResponseEntity<WebsiteVersionResponseDTO> publish(@PathVariable Long ownerId,@PathVariable Long versionId,@RequestHeader(value="If-Match",required=false) String ifMatch,@RequestHeader(value="Idempotency-Key",required=false) String idempotencyKey){
        var result=service.publishNow(ownerId,versionId,HttpEntityTag.requireRevision(ifMatch,"version",versionId),idempotencyKey);
        return ResponseEntity.ok().eTag(HttpEntityTag.version(result.id(),result.contentRevision())).body(result);
    }
    @PutMapping("/schedule")
    public ResponseEntity<WebsiteVersionResponseDTO> schedule(@PathVariable Long ownerId,@PathVariable Long versionId,@RequestHeader(value="If-Match",required=false) String ifMatch,@RequestBody @Valid PublicationScheduleRequest request){
        var result=service.schedule(ownerId,versionId,HttpEntityTag.requireRevision(ifMatch,"version",versionId),sorbonne.professional_website.time.PlatformTime.toUtcLocal(request.publishAt()));
        return ResponseEntity.ok().eTag(HttpEntityTag.version(result.id(),result.contentRevision())).body(result);
    }
    @PostMapping("/rollback")
    public ResponseEntity<WebsiteVersionResponseDTO> rollback(@PathVariable Long ownerId,@PathVariable Long versionId,@RequestHeader(value="If-Match",required=false) String ifMatch){
        var result=service.rollbackTo(ownerId,versionId,HttpEntityTag.requireRevision(ifMatch,"version",versionId));
        return ResponseEntity.ok().eTag(HttpEntityTag.version(result.id(),result.contentRevision())).body(result);
    }
    @DeleteMapping("/schedule")
    public ResponseEntity<WebsiteVersionResponseDTO> cancel(@PathVariable Long ownerId,@PathVariable Long versionId,@RequestHeader(value="If-Match",required=false) String ifMatch){
        var result=service.cancelSchedule(ownerId,versionId,HttpEntityTag.requireRevision(ifMatch,"version",versionId));
        return ResponseEntity.ok().eTag(HttpEntityTag.version(result.id(),result.contentRevision())).body(result);
    }
}
