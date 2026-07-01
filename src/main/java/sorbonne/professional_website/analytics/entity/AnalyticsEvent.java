package sorbonne.professional_website.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "page_path", length = 1000)
    private String pagePath;

    @Column(name = "page_title", length = 300)
    private String pageTitle;

    @Column(name = "project_slug", length = 255)
    private String projectSlug;

    @Column(name = "referrer", length = 1200)
    private String referrer;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "medium", length = 255)
    private String medium;

    @Column(name = "campaign", length = 255)
    private String campaign;

    @Column(name = "recruiter_code", length = 255)
    private String recruiterCode;

    @Column(name = "visitor_id_hash", length = 128)
    private String visitorIdHash;

    @Column(name = "session_id_hash", length = 128)
    private String sessionIdHash;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "browser", length = 120)
    private String browser;

    @Column(name = "os", length = 120)
    private String os;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "screen_width")
    private Integer screenWidth;

    @Column(name = "screen_height")
    private Integer screenHeight;

    @Column(name = "user_agent", length = 600)
    private String userAgent;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
