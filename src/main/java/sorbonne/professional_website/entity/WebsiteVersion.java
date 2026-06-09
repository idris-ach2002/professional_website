package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "website_version",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_owner_version_tag",
                        columnNames = {"owner_id", "version_tag"}
                )
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class WebsiteVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "website_version_seq")
    @SequenceGenerator(
            name = "website_version_seq",
            sequenceName = "website_version_seq",
            allocationSize = 1
    )
    @Column(name = "website_version_id")
    private Long id;

    @Column(name = "version_tag", nullable = false, length = 80)
    private String versionTag;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "timeline_id")
    private Timeline timeline;

    @OneToMany(
            mappedBy = "websiteVersion",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC, startDate DESC")
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    public void attachProfile(Profile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setWebsiteVersion(this);
        }
    }

    public void attachTimeline(Timeline timeline) {
        this.timeline = timeline;
        if (timeline != null) {
            timeline.setWebsiteVersion(this);
        }
    }

    public void addProject(Project project) {
        if (project == null) {
            return;
        }

        project.setWebsiteVersion(this);
        this.projects.add(project);
    }

    public void clearAndAttachProjects(List<Project> projects) {
        this.projects.clear();

        if (projects == null) {
            return;
        }

        for (Project project : projects) {
            addProject(project);
        }
    }
}
