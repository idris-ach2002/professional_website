package sorbonne.professional_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}