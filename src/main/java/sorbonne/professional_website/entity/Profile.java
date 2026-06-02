package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_seq")
    @SequenceGenerator(
            name = "profile_seq",
            sequenceName = "profile_seq",
            allocationSize = 1
    )
    @Column(name = "profile_id")
    private Long id;

    @Column(length = 120, nullable = false)
    private String title;

    @Column(length = 180)
    private String subtitle;

    @Column(length = 256)
    private String headline;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 120)
    private String location;

    @Column(length = 160)
    private String availability;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "cv_url", length = 512)
    private String cvUrl;

    @Column(name = "portfolio_url", length = 512)
    private String portfolioUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "prof", cascade = CascadeType.ALL)
    private User user;
}