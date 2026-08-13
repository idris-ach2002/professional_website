package sorbonne.professional_website.events;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event", uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_key", columnNames = "event_key"), indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status,created_at"),
        @Index(name = "idx_outbox_owner_created", columnList = "owner_id,created_at")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OutboxEvent {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "event_key", nullable = false, updatable = false, length = 180)
    private String eventKey;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void initialize() {
        if (id == null) id = UUID.randomUUID().toString();
        if (status == null) status = OutboxStatus.PENDING;
        LocalDateTime now = sorbonne.professional_website.time.PlatformTime.utcNow();
        if (nextAttemptAt == null) nextAttemptAt = now;
        if (createdAt == null) createdAt = now;
    }
}
