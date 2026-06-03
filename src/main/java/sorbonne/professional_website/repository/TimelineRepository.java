package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.Timeline;

public interface TimelineRepository extends JpaRepository<Timeline, Long> {

}
