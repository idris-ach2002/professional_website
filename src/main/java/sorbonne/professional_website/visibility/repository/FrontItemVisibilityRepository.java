package sorbonne.professional_website.visibility.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.visibility.entity.FrontItemVisibility;

public interface FrontItemVisibilityRepository extends JpaRepository<FrontItemVisibility, String> {
}
