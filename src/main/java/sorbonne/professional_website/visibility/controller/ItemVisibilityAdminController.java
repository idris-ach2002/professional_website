package sorbonne.professional_website.visibility.controller;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorbonne.professional_website.visibility.dto.ItemVisibilityResponse;
import sorbonne.professional_website.visibility.dto.ItemVisibilityUpdateRequest;
import sorbonne.professional_website.visibility.service.ItemVisibilityService;

@RestController
@RequestMapping("/api/items-visibility")
public class ItemVisibilityAdminController {

    private final ItemVisibilityService service;

    public ItemVisibilityAdminController(ItemVisibilityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ItemVisibilityResponse> getVisibility() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.snapshot());
    }

    @PutMapping
    public ResponseEntity<ItemVisibilityResponse> replace(@RequestBody @Valid ItemVisibilityUpdateRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.replaceOverrides(request.items()));
    }
}
