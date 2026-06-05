package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.Owner;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

}