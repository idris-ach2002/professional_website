package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;
import sorbonne.professional_website.entity.enumerations.ProjectLinkType;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectLink {

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 50)
    private ProjectLinkType type;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "url", nullable = false, length = 512)
    private String url;
}