package ru.nsu.crossfitbuddy.core.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.nsu.crossfitbuddy.core.entity.S3File;

public interface S3FileRepository extends JpaRepository<S3File, UUID> {
    @Query("""
                SELECT f FROM S3File f
                WHERE f.id = :id
                  AND (f.status <> 'PENDING' OR f.expireAt > CURRENT_TIMESTAMP)
            """)
    public Optional<S3File> findByIdReady(@Param("id") UUID id);
}
