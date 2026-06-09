package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    @Builder.Default
    private Boolean active = true;

    @Column(length = 256, nullable = false)
    private String address;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "owner_contacts",
            joinColumns = @JoinColumn(name = "owner_id")
    )
    @Builder.Default
    private List<ContactInfo> contacts = new ArrayList<>();

    /**
     * History of the owner's website versions.
     *
     * Owner = identity/account.
     * WebsiteVersion = complete snapshot of the public website content.
     */
    @OneToMany(
            mappedBy = "owner",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<WebsiteVersion> websiteVersions = new ArrayList<>();

    public Optional<WebsiteVersion> getActiveWebsiteVersion() {
        if (websiteVersions == null) {
            return Optional.empty();
        }

        return websiteVersions.stream()
                .filter(version -> Boolean.TRUE.equals(version.getActive()))
                .findFirst();
    }
}
