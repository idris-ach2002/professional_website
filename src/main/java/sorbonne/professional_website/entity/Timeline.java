package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timeline")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Timeline {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "timeline_seq")
    @SequenceGenerator(
            name = "timeline_seq",
            sequenceName = "timeline_seq",
            allocationSize = 1
    )
    @Column(name = "timeline_id")
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 300)
    private String description;

    @OneToMany(
            mappedBy = "timeline",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("startDate DESC")
    @Builder.Default
    private List<Experience> experiences = new ArrayList<>();

    @OneToOne(mappedBy = "timeline")
    private WebsiteVersion websiteVersion;
}