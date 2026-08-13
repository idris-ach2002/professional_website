package sorbonne.professional_website.publication;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/{ownerId}/versions")
public class VersionDiffAdminController {
    private final VersionDiffService service; public VersionDiffAdminController(VersionDiffService service){this.service=service;}
    @GetMapping("/{fromVersionId}/diff/{toVersionId}")
    public ResponseEntity<VersionDiffResponse> diff(@PathVariable Long ownerId,@PathVariable Long fromVersionId,@PathVariable Long toVersionId){ return ResponseEntity.ok(service.diff(ownerId,fromVersionId,toVersionId)); }
}
