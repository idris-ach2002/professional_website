package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;
import sorbonne.professional_website.entity.enumerations.ProjectLinkType;
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

    @Column(name = "architecture_url", length = 512)
    private String architectureUrl;

    @Column(name = "documentation_url", length = 512)
    private String documentationUrl;

    @Column(name = "slug", length = 180)
    private String slug;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_proof_tags",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "proof_tag", length = 100, nullable = false)
    @Builder.Default
    private List<String> proofTags = new ArrayList<>();

    @Column(name = "case_study_problem", columnDefinition = "TEXT")
    private String caseStudyProblem;

    @Column(name = "case_study_context", columnDefinition = "TEXT")
    private String caseStudyContext;

    @Column(name = "case_study_role", columnDefinition = "TEXT")
    private String caseStudyRole;

    @Column(name = "case_study_architecture", columnDefinition = "TEXT")
    private String caseStudyArchitecture;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_case_study_technical_choices",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "technical_choice", length = 500, nullable = false)
    @Builder.Default
    private List<String> caseStudyTechnicalChoices = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_case_study_challenges",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "challenge", length = 500, nullable = false)
    @Builder.Default
    private List<String> caseStudyChallenges = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_case_study_solutions",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "solution", length = 500, nullable = false)
    @Builder.Default
    private List<String> caseStudySolutions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_case_study_outcomes",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "outcome", length = 500, nullable = false)
    @Builder.Default
    private List<String> caseStudyOutcomes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_case_study_results",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "result", length = 500, nullable = false)
    @Builder.Default
    private List<String> caseStudyResults = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_case_study_limits",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "project_limit", length = 500, nullable = false)
    @Builder.Default
    private List<String> caseStudyLimits = new ArrayList<>();

    @Column(name = "case_study_next_steps", columnDefinition = "TEXT")
    private String caseStudyNextSteps;

    @Column(name = "featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "website_version_id", nullable = false)
    private WebsiteVersion websiteVersion;


    @Embeddable
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectLink {

        @Enumerated(EnumType.STRING)
        @Column(name = "link_type", nullable = false, length = 50)
        private ProjectLinkType type;

        @Column(name = "label", length = 100)
        private String label;

        @Column(name = "url", nullable = false, length = 512)
        private String url;
    }

}