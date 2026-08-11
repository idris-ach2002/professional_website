package sorbonne.professional_website.analytics.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AnalyticsRateLimiter {

    private static final long WINDOW_MS = 60_000L;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerMinute;
    private final Clock clock;
    private final AtomicLong lastCleanup = new AtomicLong(0L);

    @Autowired
    public AnalyticsRateLimiter(@Value("${app.analytics.max-events-per-minute:120}") int maxPerMinute) {
        this(maxPerMinute, Clock.systemUTC());
    }

    AnalyticsRateLimiter(int maxPerMinute, Clock clock) {
        this.maxPerMinute = Math.max(10, maxPerMinute);
        this.clock = clock;
    }

    public boolean allow(HttpServletRequest request) {
        long now = clock.millis();
        String key = hash(clientKey(request));
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_MS) return new Window(now);
            return current;
        });
        boolean allowed = window.count.incrementAndGet() <= maxPerMinute;
        long previousCleanup = lastCleanup.get();
        if (windows.size() > 4096
                && now - previousCleanup >= WINDOW_MS
                && lastCleanup.compareAndSet(previousCleanup, now)) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MS * 2);
        }
        return allowed;
    }

    private static String clientKey(HttpServletRequest request) {
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (cloudflare != null && !cloudflare.isBlank()) return cloudflare.trim();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].trim();
        return String.valueOf(request.getRemoteAddr());
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
