package sorbonne.professional_website.jobs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/manager/{ownerId}/jobs")
public class BackgroundJobAdminController {
    private final BackgroundJobService service; public BackgroundJobAdminController(BackgroundJobService service){this.service=service;}
    @GetMapping public ResponseEntity<List<BackgroundJobResponse>> list(@PathVariable Long ownerId){ return ResponseEntity.ok(service.list(ownerId)); }
    @PutMapping("/{jobId}/cancel") public ResponseEntity<BackgroundJobResponse> cancel(@PathVariable Long ownerId,@PathVariable String jobId){ return ResponseEntity.ok(service.cancel(ownerId,jobId)); }
    @PutMapping("/{jobId}/retry") public ResponseEntity<BackgroundJobResponse> retry(@PathVariable Long ownerId,@PathVariable String jobId){ return ResponseEntity.ok(service.retry(ownerId,jobId)); }
}
