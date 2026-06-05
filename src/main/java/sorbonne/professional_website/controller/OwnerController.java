package sorbonne.professional_website.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.service.OwnerService;

import java.util.List;

@RestController
@RequestMapping("/manager")
public class OwnerController {

    private final OwnerService srvOwner;

    public OwnerController(OwnerService srvOwner) {
        this.srvOwner = srvOwner;
    }

    @PostMapping
    public ResponseEntity<Void> createOwner(@RequestBody @Valid OwnerRequestDTO ownerRequestDTO) {
        srvOwner.createOwner(ownerRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<OwnerResponseDTO>> getAllOwners() {
        return ResponseEntity.ok(srvOwner.getAllOwners());
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> getOwnerById(@PathVariable Long ownerId) {
        return ResponseEntity.ok(srvOwner.getOwnerById(ownerId));
    }

    @PutMapping("/{ownerId}")
    public ResponseEntity<Void> updateOwner(
            @PathVariable Long ownerId,
            @RequestBody @Valid OwnerRequestDTO ownerRequestDTO
    ) {
        srvOwner.updateOwner(ownerId, ownerRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> deleteOwner(@PathVariable Long ownerId) {
        srvOwner.deleteOwner(ownerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ownerId}/profile")
    public ResponseEntity<Void> createOrReplaceProfile(
            @PathVariable Long ownerId,
            @RequestBody @Valid ProfileRequestDTO profileRequestDTO
    ) {
        srvOwner.createOrReplaceProfile(ownerId, profileRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{ownerId}/profile")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long ownerId,
            @RequestBody @Valid ProfileRequestDTO profileRequestDTO
    ) {
        srvOwner.createOrReplaceProfile(ownerId, profileRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ownerId}/timeline")
    public ResponseEntity<Void> createOrReplaceTimeline(
            @PathVariable Long ownerId,
            @RequestBody @Valid TimelineRequestDTO timelineRequestDTO
    ) {
        srvOwner.createOrReplaceTimeline(ownerId, timelineRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{ownerId}/timeline")
    public ResponseEntity<Void> updateTimeline(
            @PathVariable Long ownerId,
            @RequestBody @Valid TimelineRequestDTO timelineRequestDTO
    ) {
        srvOwner.createOrReplaceTimeline(ownerId, timelineRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ownerId}/projects")
    public ResponseEntity<Void> addProjectToOwner(
            @PathVariable Long ownerId,
            @RequestBody @Valid ProjectRequestDTO projectRequestDTO
    ) {
        srvOwner.addProjectToOwner(ownerId, projectRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
