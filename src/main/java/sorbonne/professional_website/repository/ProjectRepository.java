package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByWebsiteVersion_IdOrderByDisplayOrderAscStartDateDesc(Long websiteVersionId);

    Optional<Project> findByIdAndWebsiteVersion_Id(Long projectId, Long websiteVersionId);
}
