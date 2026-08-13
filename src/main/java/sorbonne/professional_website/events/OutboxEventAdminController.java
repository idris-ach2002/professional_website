package sorbonne.professional_website.events;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager/{ownerId}/events")
public class OutboxEventAdminController {
    private final OutboxEventRepository repository;
    private final OutboxDispatchService dispatchService;

    public OutboxEventAdminController(OutboxEventRepository repository, OutboxDispatchService dispatchService) {
        this.repository = repository;
        this.dispatchService = dispatchService;
    }

    @GetMapping
    public ResponseEntity<List<OutboxEventResponse>> list(@PathVariable Long ownerId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(repository.findTop100ByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(OutboxEventResponse::from).toList());
    }

    @PutMapping("/{eventId}/retry")
    public ResponseEntity<OutboxEventResponse> retryDead(@PathVariable Long ownerId, @PathVariable String eventId) {
        return ResponseEntity.ok(dispatchService.retryDead(ownerId, eventId));
    }
}
