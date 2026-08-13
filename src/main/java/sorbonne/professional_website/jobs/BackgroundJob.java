package sorbonne.professional_website.jobs;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "background_job", indexes = {
        @Index(name = "idx_background_job_owner_created", columnList = "owner_id,created_at"),
        @Index(name = "idx_background_job_status_execute", columnList = "status,execute_after")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BackgroundJob {
    @Id
    @Column(name = "job_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "version_id")
    private Long versionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BackgroundJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BackgroundJobStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 50;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "execute_after")
    private LocalDateTime executeAfter;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "heartbeat_at")
    private LocalDateTime heartbeatAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void initialize() {
        if (id == null) id = UUID.randomUUID().toString();
        if (status == null) status = BackgroundJobStatus.QUEUED;
        if (maxAttempts <= 0) maxAttempts = 3;
        progress = Math.max(0, Math.min(100, progress));
        LocalDateTime now = sorbonne.professional_website.time.PlatformTime.utcNow();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = sorbonne.professional_website.time.PlatformTime.utcNow();
    }
}
