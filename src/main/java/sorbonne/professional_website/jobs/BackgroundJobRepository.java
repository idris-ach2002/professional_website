package sorbonne.professional_website.jobs;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, String> {
    long countByStatus(BackgroundJobStatus status);
    Page<BackgroundJob> findByStatusIn(List<BackgroundJobStatus> statuses, Pageable pageable);
    List<BackgroundJob> findTop100ByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<BackgroundJob> findTop50ByTypeAndStatusInAndExecuteAfterLessThanEqualOrderByPriorityDescExecuteAfterAsc(
            BackgroundJobType type, List<BackgroundJobStatus> statuses, LocalDateTime now);
    Optional<BackgroundJob> findByIdAndOwnerId(String id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from BackgroundJob j where j.id = :id")
    Optional<BackgroundJob> lockById(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j from BackgroundJob j
            where j.status = :status
              and j.heartbeatAt is not null
              and j.heartbeatAt <= :cutoff
            order by j.heartbeatAt asc
            """)
    List<BackgroundJob> lockStaleRunning(@Param("status") BackgroundJobStatus status, @Param("cutoff") LocalDateTime cutoff);

    List<BackgroundJob> findByOwnerIdAndVersionIdAndTypeAndStatusIn(
            Long ownerId, Long versionId, BackgroundJobType type, List<BackgroundJobStatus> statuses);
}
