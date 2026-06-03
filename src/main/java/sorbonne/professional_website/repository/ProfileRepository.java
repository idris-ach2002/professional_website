package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
