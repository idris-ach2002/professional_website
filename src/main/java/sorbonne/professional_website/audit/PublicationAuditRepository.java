package sorbonne.professional_website.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicationAuditRepository extends JpaRepository<PublicationAuditEntry, String> {
    List<PublicationAuditEntry> findTop200ByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<PublicationAuditEntry> findTop200ByOwnerIdAndVersionIdOrderByCreatedAtDesc(Long ownerId, Long versionId);
}
