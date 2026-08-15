package sorbonne.professional_website.visibility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.visibility.dto.ItemVisibilityResponse;
import sorbonne.professional_website.visibility.entity.FrontItemVisibility;
import sorbonne.professional_website.visibility.repository.FrontItemVisibilityRepository;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ItemVisibilityService {

    private static final Pattern SAFE_KEY = Pattern.compile("[a-z0-9][a-z0-9._:-]{0,179}");
    private final FrontItemVisibilityRepository repository;

    public ItemVisibilityService(FrontItemVisibilityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ItemVisibilityResponse snapshot() {
        Map<String, Boolean> items = new LinkedHashMap<>();
        OffsetDateTime latest = null;
        for (FrontItemVisibility entity : repository.findAll()) {
            items.put(entity.getItemKey(), entity.isVisible());
            if (entity.getUpdatedAt() != null && (latest == null || entity.getUpdatedAt().isAfter(latest))) {
                latest = entity.getUpdatedAt();
            }
        }
        return new ItemVisibilityResponse(Map.copyOf(items), latest);
    }

    @Transactional
    public ItemVisibilityResponse replaceOverrides(Map<String, Boolean> requestedItems) {
        Map<String, Boolean> safe = new LinkedHashMap<>();
        requestedItems.forEach((rawKey, rawVisible) -> {
            String key = normalizeKey(rawKey);
            boolean visible = rawVisible == null || rawVisible;
            safe.put(key, visible);
        });

        repository.deleteAllInBatch();
        safe.forEach((key, visible) -> {
            // True is the platform default. Persist only exceptions so new frontend
            // items remain visible automatically until an administrator hides them.
            if (!visible) repository.save(new FrontItemVisibility(key, false));
        });
        repository.flush();
        return snapshot();
    }

    private String normalizeKey(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim().toLowerCase();
        if (!SAFE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid item visibility key: " + rawKey);
        }
        return key;
    }
}
