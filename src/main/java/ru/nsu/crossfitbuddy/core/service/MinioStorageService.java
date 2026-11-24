package ru.nsu.crossfitbuddy.core.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public String upload(String objectKey, MultipartFile file) throws MinioException, IOException {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .stream(file.getInputStream(), file.getSize(), -1L)
                        .contentType(file.getContentType())
                        .build());
        return objectKey;
    }

    public String pending(String objectKey) throws MinioException {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectKey)
                        .expiry(10, TimeUnit.MINUTES)
                        .build());
    }

    public boolean isObjectExist(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new RuntimeException("Ошибка при проверке файла в MinIO", e);
        } catch (Exception e) {
            throw new RuntimeException("Непредвиденная ошибка при работе с MinIO", e);
        }
    }

    public String getDownloadUrl(String objectName)
            throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(10, TimeUnit.MINUTES)
                        .build());
    }

    public void delete(String objectKey) throws MinioException {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build());
    }

    public byte[] download(String objectKey) throws MinioException, IOException {
        try (var stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build())) {
            return stream.readAllBytes();
        }
    }
}
