package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sorbonne.professional_website.entity.WebsiteVersion;

import java.util.List;
import java.util.Optional;

public interface WebsiteVersionRepository extends JpaRepository<WebsiteVersion, Long> {

    List<WebsiteVersion> findByOwnerOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<WebsiteVersion> findByIdAndOwnerOwnerId(Long versionId, Long ownerId);

    Optional<WebsiteVersion> findByOwnerOwnerIdAndActiveTrue(Long ownerId);

    boolean existsByOwnerOwnerIdAndActiveTrue(Long ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update WebsiteVersion w
        set w.active = false
        where w.owner.ownerId = :ownerId
    """)
    void deactivateAllByOwnerId(@Param("ownerId") Long ownerId);
}
