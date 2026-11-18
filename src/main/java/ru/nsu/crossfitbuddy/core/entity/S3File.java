package ru.nsu.crossfitbuddy.core.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "s3_file")
@Getter
@Setter
public class S3File {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fileName;

    @Enumerated(EnumType.STRING)
    private MimoType type;
    private Long size;

    private String objectKey;

    @Enumerated(EnumType.STRING)
    private S3FileStatus status;

    private Instant expireAt;

}
