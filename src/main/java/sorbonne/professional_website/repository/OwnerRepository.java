package sorbonne.professional_website.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sorbonne.professional_website.entity.Owner;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Owner o
        where o.ownerId = :ownerId
    """)
    Optional<Owner> lockByOwnerId(@Param("ownerId") Long ownerId);
}
