package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sorbonne.professional_website.entity.WebsiteVersion;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import sorbonne.professional_website.publication.PublicationStatus;

public interface WebsiteVersionRepository extends JpaRepository<WebsiteVersion, Long> {
    long countByPublicationStatus(PublicationStatus publicationStatus);

    List<WebsiteVersion> findByOwnerOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<WebsiteVersion> findByIdAndOwnerOwnerId(Long versionId, Long ownerId);

    Optional<WebsiteVersion> findByOwnerOwnerIdAndActiveTrue(Long ownerId);

    Optional<WebsiteVersion> findByOwnerOwnerIdAndActiveTrueAndPublishedTrue(Long ownerId);

    List<WebsiteVersion> findTop50ByPublicationStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            PublicationStatus publicationStatus, LocalDateTime now);

    boolean existsByOwnerOwnerIdAndActiveTrue(Long ownerId);

    long countByOwnerOwnerId(Long ownerId);

    @Modifying(flushAutomatically = true)
    @Query("""
        update WebsiteVersion w
        set w.active = false
        where w.owner.ownerId = :ownerId
    """)
    void deactivateAllByOwnerId(@Param("ownerId") Long ownerId);

    @Modifying(flushAutomatically = true)
    @Query("""
        update WebsiteVersion w
        set w.active = false
        where w.owner.ownerId = :ownerId
          and w.id <> :versionId
          and w.active = true
    """)
    void deactivateOthersByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("versionId") Long versionId
    );
}
