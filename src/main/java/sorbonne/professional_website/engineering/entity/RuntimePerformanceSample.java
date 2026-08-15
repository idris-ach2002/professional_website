package sorbonne.professional_website.engineering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "runtime_performance_sample")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimePerformanceSample {

    @Id
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "build_id", nullable = false, length = 120)
    private String buildId;

    @Column(name = "runtime_profile", nullable = false, length = 24)
    private String runtimeProfile;

    @Column(name = "memory_state", nullable = false, length = 24)
    private String memoryState;

    private Double fps;

    @Column(name = "frame_p95_ms")
    private Double frameP95Ms;

    @Column(name = "long_task_count", nullable = false)
    private int longTaskCount;

    @Column(name = "worker_latency_ms")
    private Double workerLatencyMs;

    @Column(name = "api_latency_ms")
    private Double apiLatencyMs;

    @Column(name = "active_resources", nullable = false)
    private int activeResources;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (recordedAt == null) recordedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
