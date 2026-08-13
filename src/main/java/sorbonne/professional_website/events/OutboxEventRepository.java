package sorbonne.professional_website.events;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop100ByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<OutboxEvent> findByEventKey(String eventKey);
    Optional<OutboxEvent> findByIdAndOwnerId(String id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from OutboxEvent e
            where e.status = :status
              and e.nextAttemptAt <= :now
            order by e.createdAt asc
            """)
    List<OutboxEvent> lockDue(@Param("status") OutboxStatus status, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from OutboxEvent e
            where e.status = :status
              and e.claimedAt is not null
              and e.claimedAt <= :cutoff
            order by e.claimedAt asc
            """)
    List<OutboxEvent> lockStaleProcessing(@Param("status") OutboxStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.id = :id")
    Optional<OutboxEvent> lockById(@Param("id") String id);
}
