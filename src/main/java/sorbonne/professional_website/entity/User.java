package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(
            name = "user_seq",
            sequenceName = "user_seq",
            allocationSize = 1
    )
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 256, nullable = false)
    private String name;

    @Column(name = "first_name", length = 256, nullable = false)
    private String firstName;

    @Column(nullable = false)
    private int age;

    @Column(length = 256, nullable = false)
    private String address;

    // Contacts association
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_contacts",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<ContactInfo> contacts = new ArrayList<>();

    // Profile association
    @OneToOne
    @JoinColumn(name="profile_id")
    private Profile prof;

    // timeline (school, company, works ..) association
    @OneToOne
    @JoinColumn(name="timeline_id")
    private Timeline timeline;

    // Project association
    @OneToMany(targetEntity = Project.class, mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

}