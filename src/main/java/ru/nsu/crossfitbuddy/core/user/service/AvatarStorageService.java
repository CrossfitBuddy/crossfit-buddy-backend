package ru.nsu.crossfitbuddy.core.user.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.crossfitbuddy.core.exception.ApiException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public String upload(UUID userId, MultipartFile file) {
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                "Failed to upload avatar to MinIO: " + e.getMessage());
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                "Failed to delete avatar from MinIO: " + e.getMessage());
        }
    }

    public byte[] download(String objectKey) {
        try (var stream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                "Failed to download avatar from MinIO: " + e.getMessage());
        }
    }
}
