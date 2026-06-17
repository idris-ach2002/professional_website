package sorbonne.professional_website.applications.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_application")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "job_application_seq")
    @SequenceGenerator(
            name = "job_application_seq",
            sequenceName = "job_application_seq",
            allocationSize = 1
    )
    @Column(name = "job_application_id")
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "website_version_id")
    private Long versionId;

    @Column(name = "company_name", length = 180, nullable = false)
    private String companyName;

    @Column(name = "role_title", length = 220, nullable = false)
    private String roleTitle;

    @Column(length = 180)
    private String location;

    @Column(name = "offer_url", length = 1000)
    private String offerUrl;

    @Lob
    @Column(name = "offer_text")
    private String offerText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "target_profile", length = 160)
    private String targetProfile;

    @Column(name = "cv_variant_name", length = 160)
    private String cvVariantName;

    @Column(name = "cv_url", length = 1000)
    private String cvUrl;

    @Column(name = "cover_letter_url", length = 1000)
    private String coverLetterUrl;

    @Column(name = "application_zip_url", length = 1000)
    private String applicationZipUrl;

    @Lob
    @Column(name = "mail_draft")
    private String mailDraft;

    @Lob
    @Column(name = "cover_letter_source")
    private String coverLetterSource;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Column(name = "relevance_score")
    private Integer relevanceScore;

    @Lob
    @Column(name = "matched_keywords")
    private String matchedKeywords;

    @Lob
    @Column(name = "missing_keywords")
    private String missingKeywords;

    @Lob
    @Column(name = "recommendations")
    private String recommendations;

    @Column(name = "applied_at")
    private LocalDate appliedAt;

    @Column(name = "follow_up_at")
    private LocalDate followUpAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
