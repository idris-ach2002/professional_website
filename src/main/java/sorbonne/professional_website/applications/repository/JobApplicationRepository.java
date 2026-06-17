package sorbonne.professional_website.applications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.applications.entity.ApplicationStatus;
import sorbonne.professional_website.applications.entity.JobApplication;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    List<JobApplication> findByOwnerIdAndStatusOrderByUpdatedAtDesc(Long ownerId, ApplicationStatus status);

    Optional<JobApplication> findByIdAndOwnerId(Long id, Long ownerId);
}
