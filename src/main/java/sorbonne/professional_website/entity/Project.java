package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;
import sorbonne.professional_website.entity.enumerations.ProjectStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq")
    @SequenceGenerator(
            name = "project_seq",
            sequenceName = "project_seq",
            allocationSize = 1
    )
    @Column(name = "project_id")
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "demo_url", length = 512)
    private String demoUrl;

    @Column(name = "github_url", length = 512)
    private String githubUrl;

    @Column(name = "documentation_url", length = 512)
    private String documentationUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_stacks",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "stack", length = 100, nullable = false)
    @Builder.Default
    private List<String> stacks = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_features",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "feature", length = 255, nullable = false)
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_links",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Builder.Default
    private List<ProjectLink> links = new ArrayList<>();

    @Column(name = "featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="user_id")
    private User user;
}