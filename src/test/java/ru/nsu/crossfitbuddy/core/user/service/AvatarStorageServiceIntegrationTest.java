package ru.nsu.crossfitbuddy.core.user.service;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class AvatarStorageServiceIntegrationTest {

    @Container
    static MinIOContainer minio = new MinIOContainer(
        DockerImageName.parse("pgsty/minio:latest").asCompatibleSubstituteFor("minio/minio")
    );

    @Autowired
    private AvatarStorageService avatarStorageService;

    @Autowired
    private MinioClient minioClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.minio.url", minio::getS3URL);
        registry.add("app.minio.access-key", minio::getUserName);
        registry.add("app.minio.secret-key", minio::getPassword);
        registry.add("app.minio.bucket-name", () -> "test-bucket");
    }

    @BeforeEach
    void setUp() throws Exception {
        if (!minioClient.bucketExists(
            io.minio.BucketExistsArgs.builder().bucket("test-bucket").build())) {
            minioClient.makeBucket(
                io.minio.MakeBucketArgs.builder().bucket("test-bucket").build());
        }
    }

    @Test
    void uploadAndDelete_shouldWorkWithRealMinio() throws Exception {
        UUID userId = UUID.randomUUID();
        byte[] imageBytes = "fake-image-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.jpg", "image/jpeg", imageBytes);

        String objectKey = avatarStorageService.upload(userId, file);
        assertNotNull(objectKey);

        byte[] downloaded = avatarStorageService.download(objectKey);
        assertArrayEquals(imageBytes, downloaded);

        avatarStorageService.delete(objectKey);

        assertThrows(Exception.class, () ->
            avatarStorageService.download(objectKey));
    }
}
