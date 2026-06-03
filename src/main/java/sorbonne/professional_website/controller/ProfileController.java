package sorbonne.professional_website.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.response.ProfileResponseDTO;
import sorbonne.professional_website.service.ProfileService;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService srvProfile;

    public ProfileController(ProfileService srvProfile) {
        this.srvProfile = srvProfile;
    }

    @PostMapping
    public ResponseEntity<Void> createProfile(@RequestBody ProfileRequestDTO profileRequestDTO) {
        srvProfile.createProfile(profileRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponseDTO>> getAllProfiles() {
        return ResponseEntity.ok(srvProfile.getAllProfiles());
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponseDTO> getProfileById(@PathVariable Long profileId) {
        return ResponseEntity.ok(srvProfile.getProfileById(profileId));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long profileId,
            @RequestBody ProfileRequestDTO profileRequestDTO
    ) {
        srvProfile.updateProfile(profileId, profileRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long profileId) {
        srvProfile.deleteProfile(profileId);
        return ResponseEntity.noContent().build();
    }
}
