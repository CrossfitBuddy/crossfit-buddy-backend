package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.crossfitbuddy.core.exception.ApiException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private final MinioStorageService minioStorageService;

    public String upload(UUID userId, MultipartFile file) {
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID() + "_"
                + file.getOriginalFilename();
        try {
            minioStorageService.upload(objectKey, file);
            return objectKey;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to upload avatar to MinIO: " + e.getMessage());
        }
    }

    public void delete(String objectKey) {
        try {
            minioStorageService.delete(objectKey);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to delete avatar from MinIO: " + e.getMessage());
        }
    }

    public byte[] download(String objectKey) {
        try {
            return minioStorageService.download(objectKey);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to download avatar from MinIO: " + e.getMessage());
        }
    }
}
