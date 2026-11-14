package ru.nsu.crossfitbuddy.core.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.exception.ApiException;

@Service
@RequiredArgsConstructor
public class ExerciseFileStorageService {

    private final MinioStorageService minioStorageService;

    public String uploadVideo(UUID exerciseId, MultipartFile file) {
        String objectKey = "video/" + exerciseId + "/" + UUID.randomUUID() + "_"
                + file.getOriginalFilename();
        try {
            minioStorageService.upload(objectKey, file);
            return objectKey;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to upload video to MinIO: " + e.getMessage());
        }
    }

    public String uploadImage(UUID exerciseId, MultipartFile file) {
        String objectKey = "video/" + exerciseId + "/" + UUID.randomUUID() + "_"
                + file.getOriginalFilename();
        try {
            minioStorageService.upload(objectKey, file);
            return objectKey;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to upload image to MinIO: " + e.getMessage());
        }
    }

    public void delete(String objectKey) {
        try {
            minioStorageService.delete(objectKey);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to delete media from MinIO: " + e.getMessage());
        }
    }

    public byte[] download(String objectKey) {
        try {
            return minioStorageService.download(objectKey);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to download media from MinIO: " + e.getMessage());
        }
    }

}
