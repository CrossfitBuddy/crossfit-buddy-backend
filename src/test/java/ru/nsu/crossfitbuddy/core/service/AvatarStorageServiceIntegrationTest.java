package ru.nsu.crossfitbuddy.core.service;

import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AvatarStorageServiceIntegrationTest extends AbstractRepositoryTest {

    @Autowired
    private AvatarStorageService avatarStorageService;

    @MockitoBean
    private MinioClient minioClient;

    @Test
    void uploadAndDelete_shouldInteractWithMinioCorrectly() throws Exception {
        UUID userId = UUID.randomUUID();
        byte[] imageBytes = "fake-image-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", imageBytes);

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));

        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        String objectKey = avatarStorageService.upload(userId, file);

        assertNotNull(objectKey);
        assertTrue(objectKey.contains(userId.toString()));

        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));

        avatarStorageService.delete(objectKey);
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
    }
}
