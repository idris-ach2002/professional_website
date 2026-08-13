package sorbonne.professional_website.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Time contract for persisted platform workflows.
 *
 * <p>Publication/jobs/outbox timestamps stored as {@link LocalDateTime} are semantically UTC.
 * API boundaries that represent a user-selected instant must carry an explicit offset.</p>
 */
public final class PlatformTime {
    private PlatformTime() {}

    public static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static LocalDateTime toUtcLocal(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static OffsetDateTime asUtcOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
