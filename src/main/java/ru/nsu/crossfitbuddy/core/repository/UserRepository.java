package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);
}
