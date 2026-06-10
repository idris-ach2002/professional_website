package sorbonne.professional_website.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
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

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> getWebsiteByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(srvWebsite.getPublicWebsiteByOwnerId(ownerId));
    }
}
