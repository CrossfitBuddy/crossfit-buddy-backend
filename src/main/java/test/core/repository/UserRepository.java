package test.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import test.core.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
}
