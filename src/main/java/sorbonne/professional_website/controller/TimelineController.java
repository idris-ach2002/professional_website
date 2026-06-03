package sorbonne.professional_website.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.response.TimelineResponseDTO;
import sorbonne.professional_website.service.TimelineService;

import java.util.List;

@RestController
@RequestMapping("/api/timelines")
public class TimelineController {

    private final TimelineService srvTimeline;

    public TimelineController(TimelineService srvTimeline) {
        this.srvTimeline = srvTimeline;
    }

    @PostMapping
    public ResponseEntity<Void> createTimeline(@RequestBody @Valid TimelineRequestDTO timelineRequestDTO) {
        srvTimeline.createTimeline(timelineRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<TimelineResponseDTO>> getAllTimelines() {
        return ResponseEntity.ok(srvTimeline.getAllTimelines());
    }

    @GetMapping("/{timelineId}")
    public ResponseEntity<TimelineResponseDTO> getTimelineById(@PathVariable Long timelineId) {
        return ResponseEntity.ok(srvTimeline.getTimelineById(timelineId));
    }

    @PutMapping("/{timelineId}")
    public ResponseEntity<Void> updateTimeline(
            @PathVariable Long timelineId,
            @RequestBody @Valid TimelineRequestDTO timelineRequestDTO
    ) {
        srvTimeline.updateTimeline(timelineId, timelineRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{timelineId}")
    public ResponseEntity<Void> deleteTimeline(@PathVariable Long timelineId) {
        srvTimeline.deleteTimeline(timelineId);
        return ResponseEntity.noContent().build();
    }
}
