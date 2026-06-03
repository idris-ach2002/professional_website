package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

}
