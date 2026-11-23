package ru.nsu.crossfitbuddy.core.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "s3_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

  @Enumerated(EnumType.STRING)
  @Column(name = "visibility", nullable = false)
  private FileVisibility visibility;

  @ManyToOne
  @JoinColumn(name = "created_by", nullable = false)
  private UserAccount createdBy;

}
