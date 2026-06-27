package sorbonne.professional_website.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.ProvenSkillResponseDTO;
import sorbonne.professional_website.service.WebsiteService;

import java.util.List;

@RestController
@RequestMapping("/website")
public class WebsiteController {

    private final WebsiteService srvWebsite;

    public WebsiteController(WebsiteService srvWebsite) {
        this.srvWebsite = srvWebsite;
    }

    @GetMapping
    public ResponseEntity<List<OwnerResponseDTO>> getWebsites() {
        return ResponseEntity.ok(srvWebsite.getAllPublicWebsites());
    }

    @GetMapping("/default")
    public ResponseEntity<OwnerResponseDTO> getDefaultWebsite() {
        return ResponseEntity.ok(srvWebsite.getFirstOwner());
    }

    @GetMapping("/default/projects/{projectSlug}")
    public ResponseEntity<ProjectResponseDTO> getDefaultProjectBySlug(@PathVariable String projectSlug) {
        return ResponseEntity.ok(srvWebsite.getDefaultProjectBySlug(projectSlug));
    }

    @GetMapping("/default/proven-skills")
    public ResponseEntity<List<ProvenSkillResponseDTO>> getDefaultProvenSkills() {
        return ResponseEntity.ok(srvWebsite.getDefaultProvenSkills());
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> getWebsiteByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(srvWebsite.getPublicWebsiteByOwnerId(ownerId));
    }

    @GetMapping("/{ownerId}/projects/{projectSlug}")
    public ResponseEntity<ProjectResponseDTO> getProjectByOwnerAndSlug(
            @PathVariable Long ownerId,
            @PathVariable String projectSlug
    ) {
        return ResponseEntity.ok(srvWebsite.getProjectByOwnerAndSlug(ownerId, projectSlug));
    }

    @GetMapping("/{ownerId}/proven-skills")
    public ResponseEntity<List<ProvenSkillResponseDTO>> getProvenSkillsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(srvWebsite.getProvenSkillsByOwner(ownerId));
    }
}
