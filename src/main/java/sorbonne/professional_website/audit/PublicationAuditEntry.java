package sorbonne.professional_website.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "publication_audit", indexes = {
        @Index(name = "idx_publication_audit_owner_created", columnList = "owner_id,created_at"),
        @Index(name = "idx_publication_audit_version_created", columnList = "version_id,created_at")
})
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class PublicationAuditEntry {
    @Id
    @Column(name = "audit_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(name = "version_id", updatable = false)
    private Long versionId;

    @Column(nullable = false, length = 120, updatable = false)
    private String action;

    @Column(nullable = false, length = 160, updatable = false)
    private String actor;

    @Column(name = "correlation_id", length = 160, updatable = false)
    private String correlationId;

    @Column(name = "before_json", columnDefinition = "TEXT", updatable = false)
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "TEXT", updatable = false)
    private String afterJson;

    @Column(name = "metadata_json", columnDefinition = "TEXT", updatable = false)
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void initialize() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = sorbonne.professional_website.time.PlatformTime.utcNow();
    }
}
