package sorbonne.professional_website.visibility.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.visibility.dto.ItemVisibilityResponse;
import sorbonne.professional_website.visibility.service.ItemVisibilityService;

@RestController
@RequestMapping("/website/items-visibility")
public class ItemVisibilityPublicController {

    private final ItemVisibilityService service;

    public ItemVisibilityPublicController(ItemVisibilityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ItemVisibilityResponse> getVisibility() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(service.snapshot());
    }
}
