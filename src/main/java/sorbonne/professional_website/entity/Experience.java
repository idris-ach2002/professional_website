package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "experience")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "experience_seq")
    @SequenceGenerator(
            name = "experience_seq",
            sequenceName = "experience_seq",
            allocationSize = 1
    )
    @Column(name = "experience_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoryExperience category;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 160)
    private String organization;

    @Column(length = 160)
    private String location;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_position", nullable = false)
    @Builder.Default
    private boolean currentPosition = false;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "experience_skills",
            joinColumns = @JoinColumn(name = "experience_id")
    )
    @Column(name = "skill", length = 100, nullable = false)
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeline_id")
    private Timeline timeline;
}