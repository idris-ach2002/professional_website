package sorbonne.professional_website.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sorbonne.professional_website.entity.Owner;

import java.util.List;
import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Owner o
        where o.ownerId = :ownerId
    """)
    Optional<Owner> lockByOwnerId(@Param("ownerId") Long ownerId);

    Optional<Owner> findFirstByOrderByOwnerIdAsc();

    @Query("""
        select o
        from Owner o
        where o.active = true
          and exists (
              select w.id
              from WebsiteVersion w
              where w.owner = o
                and w.active = true
                and w.published = true
          )
        order by o.ownerId asc
    """)
    List<Owner> findAllPublicOwners();

    @Query("""
        select o
        from Owner o
        where o.ownerId = :ownerId
          and o.active = true
          and exists (
              select w.id
              from WebsiteVersion w
              where w.owner = o
                and w.active = true
                and w.published = true
          )
    """)
    Optional<Owner> findPublicOwnerById(@Param("ownerId") Long ownerId);
}
