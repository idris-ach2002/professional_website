package sorbonne.professional_website.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.service.OwnerService;
import sorbonne.professional_website.concurrency.HttpEntityTag;

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
        OwnerResponseDTO owner = srvOwner.getOwnerById(ownerId);
        return ResponseEntity.ok()
                .eTag(HttpEntityTag.owner(owner.ownerId(), owner.rowVersion()))
                .body(owner);
    }

    @PutMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> updateOwner(
            @PathVariable Long ownerId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody @Valid OwnerRequestDTO ownerRequestDTO
    ) {
        long expectedRevision = HttpEntityTag.requireRevision(ifMatch, "owner", ownerId);
        OwnerResponseDTO updated = srvOwner.updateOwner(ownerId, expectedRevision, ownerRequestDTO);
        return ResponseEntity.ok()
                .eTag(HttpEntityTag.owner(updated.ownerId(), updated.rowVersion()))
                .body(updated);
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> deleteOwner(
            @PathVariable Long ownerId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    ) {
        long expectedRevision = HttpEntityTag.requireRevision(ifMatch, "owner", ownerId);
        srvOwner.deleteOwner(ownerId, expectedRevision);
        return ResponseEntity.noContent().build();
    }
}
