package sorbonne.professional_website.engineering.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.engineering.entity.RuntimePerformanceSample;

import java.util.List;
import java.util.UUID;

public interface RuntimePerformanceSampleRepository extends JpaRepository<RuntimePerformanceSample, UUID> {
    List<RuntimePerformanceSample> findAllByOrderByRecordedAtDesc(Pageable pageable);
}
