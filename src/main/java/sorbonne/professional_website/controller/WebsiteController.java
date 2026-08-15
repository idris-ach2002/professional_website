package sorbonne.professional_website.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.PublicWebsiteSnapshotResponseDTO;
import sorbonne.professional_website.service.WebsiteService;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/website")
public class WebsiteController {

    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final WebsiteService srvWebsite;

    public WebsiteController(WebsiteService srvWebsite) {
        this.srvWebsite = srvWebsite;
    }

    @GetMapping
    public ResponseEntity<List<OwnerResponseDTO>> getWebsites(
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return cached(srvWebsite.getAllPublicWebsites(locale));
    }

    @GetMapping("/default")
    public ResponseEntity<OwnerResponseDTO> getDefaultWebsite(
            @RequestParam(defaultValue = "fr") String locale
    ) {
        long startedAt = System.nanoTime();
        OwnerResponseDTO body = srvWebsite.getFirstOwner(locale);
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .header("Server-Timing", "website;dur=" + elapsedMs(startedAt) + ";desc=\"WebsiteService.getFirstOwner\"")
                .header("X-Portfolio-Trace", String.join(">",
                        "Spring Security FilterChain",
                        "DispatcherServlet",
                        "WebsiteController",
                        "CacheInterceptor",
                        "Caffeine @Cacheable",
                        "WebsiteService proxy",
                        "Jackson"
                ))
                .body(body);
    }

    @GetMapping("/default/seo-snapshot")
    public ResponseEntity<PublicWebsiteSnapshotResponseDTO> getDefaultSeoSnapshot() {
        return cached(srvWebsite.getPublicSeoSnapshot());
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> getWebsiteByOwner(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return cached(srvWebsite.getPublicWebsiteByOwnerId(ownerId, locale));
    }

    @GetMapping("/default/projects/{projectSlug}")
    public ResponseEntity<ProjectResponseDTO> getDefaultProject(
            @PathVariable String projectSlug,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return cached(srvWebsite.getDefaultProjectBySlug(projectSlug, locale));
    }

    @GetMapping("/{ownerId}/projects/{projectSlug}")
    public ResponseEntity<ProjectResponseDTO> getProject(
            @PathVariable Long ownerId,
            @PathVariable String projectSlug,
            @RequestParam(defaultValue = "fr") String locale
    ) {
        return cached(srvWebsite.getProjectBySlug(ownerId, projectSlug, locale));
    }

    private static double elapsedMs(long startedAt) {
        return Math.round(((System.nanoTime() - startedAt) / 1_000_000.0) * 100.0) / 100.0;
    }

    private static <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(body);
    }
}
