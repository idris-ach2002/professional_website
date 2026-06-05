package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class WebsiteVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "website_seq")
    @SequenceGenerator(name = "website_seq")
    @Column(name="version")
    private Long id;

    @OneToOne
    @JoinColumn(name="owner_id")
    private Owner owner;

}
