package ru.nsu.crossfitbuddy.core.user.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import okhttp3.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AvatarStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private AvatarStorageService avatarStorageService;

    @BeforeEach
    void setUp() {
        avatarStorageService = new AvatarStorageService(minioClient);
        ReflectionTestUtils.setField(avatarStorageService, "bucketName", "test-bucket");
    }

    @Test
    void upload_shouldReturnObjectKey_whenSuccessful() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "fake-image-data".getBytes());

        String objectKey = avatarStorageService.upload(userId, file);

        assertNotNull(objectKey);
        assertTrue(objectKey.startsWith("avatars/" + userId + "/"));
        assertTrue(objectKey.endsWith("_avatar.jpg"));

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient, times(1)).putObject(captor.capture());

        PutObjectArgs args = captor.getValue();
        assertEquals("test-bucket", args.bucket());

        assertEquals(MediaType.parse("image/jpeg"), args.contentType());
    }

    @Test
    void delete_shouldCallMinioRemoveObject() throws Exception {
        String objectKey = "avatars/test-key.jpg";

        avatarStorageService.delete(objectKey);

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient, times(1)).removeObject(captor.capture());

        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals(objectKey, captor.getValue().object());
    }
}
