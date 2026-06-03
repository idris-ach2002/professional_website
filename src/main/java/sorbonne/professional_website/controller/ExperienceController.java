package sorbonne.professional_website.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.ExperienceRequestDTO;
import sorbonne.professional_website.dto.response.ExperienceResponseDTO;
import sorbonne.professional_website.service.ExperienceService;

import java.util.List;

@RestController
@RequestMapping("/api/experiences")
public class ExperienceController {

    private final ExperienceService srvExperience;

    public ExperienceController(ExperienceService srvExperience) {
        this.srvExperience = srvExperience;
    }

    @PostMapping
    public ResponseEntity<Void> createExperience(@RequestBody ExperienceRequestDTO experienceRequestDTO) {
        srvExperience.createExperience(experienceRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ExperienceResponseDTO>> getAllExperiences() {
        return ResponseEntity.ok(srvExperience.getAllExperiences());
    }

    @GetMapping("/{experienceId}")
    public ResponseEntity<ExperienceResponseDTO> getExperienceById(@PathVariable Long experienceId) {
        return ResponseEntity.ok(srvExperience.getExperienceById(experienceId));
    }

    @PutMapping("/{experienceId}")
    public ResponseEntity<Void> updateExperience(
            @PathVariable Long experienceId,
            @RequestBody ExperienceRequestDTO experienceRequestDTO
    ) {
        srvExperience.updateExperience(experienceId, experienceRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Long experienceId) {
        srvExperience.deleteExperience(experienceId);
        return ResponseEntity.noContent().build();
    }
}
