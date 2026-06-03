package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.Experience;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

}