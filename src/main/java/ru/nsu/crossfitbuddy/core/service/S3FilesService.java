package ru.nsu.crossfitbuddy.core.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.minio.errors.MinioException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.dto.request.S3UploadURLRequestDTO;
import ru.nsu.crossfitbuddy.core.dto.response.S3DownloadURLDTO;
import ru.nsu.crossfitbuddy.core.dto.response.S3UploadURLDTO;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.FileVisibility;
import ru.nsu.crossfitbuddy.core.entity.MimoType;
import ru.nsu.crossfitbuddy.core.entity.S3File;
import ru.nsu.crossfitbuddy.core.entity.S3FileStatus;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.repository.S3FileRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class S3FilesService {
  private final MinioStorageService minioStorageService;
  private final ProfileService profileService;
  private final S3FileRepository s3FileRepository;

  public S3UploadURLDTO requestPending(S3UploadURLRequestDTO request, FileVisibility visibility,
      CurrentUser user) {

    var profile = profileService.findProfileOrThrow(user.id(), true);

    var file = createFile(request, visibility, profile.getUser());

    var expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
    var expiresAtLocalTime = LocalDateTime.ofInstant(expiresAt, ZoneId.of("Europe/Moscow"));

    file.setExpireAt(expiresAt);

    String objectKey = createObjectKey(file.getId(), request.fileName(), request.contentType());

    file.setObjectKey(objectKey);

    try {
      String pendingUrl = minioStorageService.pending(objectKey);

      var response = new S3UploadURLDTO(file.getId(), pendingUrl,
          new HashMap<String, String>(),
          expiresAtLocalTime);

      return response;
    } catch (MinioException e) {
      s3FileRepository.delete(file);
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "", e.getMessage());
    }
  }

  public boolean isFileExist(UUID fileId, String fileName, MimoType contentType) {
    var objectKey = createObjectKey(fileId, fileName, contentType);
    return minioStorageService.isObjectExist(objectKey);
  }

  public void resumePending(UUID fileId) {
    var file = s3FileRepository.findById(fileId)
        .orElseThrow(() -> new EntityNotFoundException());
    file.setStatus(S3FileStatus.READY);
  }

  public S3File setFileReadyAndGet(UUID fileId) {
    var file = s3FileRepository.findByIdReadyAndNotExp(fileId)
        .orElseThrow(
            () -> new EntityNotFoundException(
                "Сущность файла c id " + fileId + " не найдена в базе"));
    if (!isFileExist(fileId, file.getFileName(), file.getType())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "", "file dose not exist in MinIo");
    }
    file.setStatus(S3FileStatus.READY);
    return file;
  }

  public S3DownloadURLDTO getDownloadLink(UUID fileId, CurrentUser user) {
    var creator = profileService.findProfileOrThrow(user.id(), true);
    var file = s3FileRepository.findByIdReadyOnly(fileId)
        .orElseThrow(
            () -> new EntityNotFoundException("Файл с id " + fileId + " не найден"));

    if (creator.getId() != file.getCreatedBy().getId()
        && file.getVisibility() != FileVisibility.PUBLIC) {
      throw new ApiException(HttpStatus.FORBIDDEN, "FILE_FORBIDEN",
          "Не достаточно прав для просмотра файла");
    }

    var objectKey = createObjectKey(fileId, file.getFileName(), file.getType());

    try {
      String url = minioStorageService.getDownloadUrl(objectKey);
      return new S3DownloadURLDTO(fileId, url, Instant.now().plus(10, ChronoUnit.MINUTES));

    } catch (Exception e) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MINIO_ERROR",
          "minio erro when try get download link" + e.getMessage());
    }

  }

  private S3File createFile(S3UploadURLRequestDTO request, FileVisibility visibility,
      UserAccount createdBy) {
    var file = new S3File();
    file.setSize(request.size());
    file.setFileName(request.fileName());
    file.setStatus(S3FileStatus.PENDING);
    file.setType(request.contentType());
    file.setVisibility(visibility);
    file.setCreatedBy(createdBy);
    file = s3FileRepository.save(file);
    return file;
  }

  private String createObjectKey(UUID id, String fileName, MimoType contentType) {
    switch (contentType) {
      case JPEG:
        return "img/" + id + "/" + fileName;
      case PNG:
        return "img/" + id + "/" + fileName;
      case MP4:
        return "vid/" + id + "/" + fileName;
      default:
        throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
            "not supported format");

    }
  }
}
