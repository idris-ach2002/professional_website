package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_owner")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "owner_seq")
    @SequenceGenerator(
            name = "owner_seq",
            sequenceName = "owner_seq",
            allocationSize = 1
    )
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(length = 256, nullable = false)
    private String name;

    @Column(name = "first_name", length = 256, nullable = false)
    private String firstName;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 256, nullable = false)
    private String address;

    // Contacts association
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "owner_contacts",
            joinColumns = @JoinColumn(name = "owner_id")
    )
    @Builder.Default
    private List<ContactInfo> contacts = new ArrayList<>();

    // Profile association
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private Profile prof;

    // Timeline association
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "timeline_id")
    private Timeline timeline;

    // Project association
    @OneToMany(targetEntity = Project.class, mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();
}
