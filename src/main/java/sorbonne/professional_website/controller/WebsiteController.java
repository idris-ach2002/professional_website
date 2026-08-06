package sorbonne.professional_website.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.PublicWebsiteSnapshotResponseDTO;
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
    public ResponseEntity<List<OwnerResponseDTO>> getWebsites(
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return ResponseEntity.ok(srvWebsite.getAllPublicWebsites(locale));
    }

    @GetMapping("/default")
    public ResponseEntity<OwnerResponseDTO> getDefaultWebsite(
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return ResponseEntity.ok(srvWebsite.getFirstOwner(locale));
    }

    @GetMapping("/default/seo-snapshot")
    public ResponseEntity<PublicWebsiteSnapshotResponseDTO> getDefaultSeoSnapshot() {
        return ResponseEntity.ok(srvWebsite.getPublicSeoSnapshot());
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> getWebsiteByOwner(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return ResponseEntity.ok(srvWebsite.getPublicWebsiteByOwnerId(ownerId, locale));
    }

    @GetMapping("/default/projects/{projectSlug}")
    public ResponseEntity<ProjectResponseDTO> getDefaultProject(
            @PathVariable String projectSlug,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return ResponseEntity.ok(srvWebsite.getDefaultProjectBySlug(projectSlug, locale));
    }

    @GetMapping("/{ownerId}/projects/{projectSlug}")
    public ResponseEntity<ProjectResponseDTO> getProject(
            @PathVariable Long ownerId,
            @PathVariable String projectSlug,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return ResponseEntity.ok(srvWebsite.getProjectBySlug(ownerId, projectSlug, locale));
    }
}
