package ru.nsu.crossfitbuddy.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ru.nsu.crossfitbuddy.core.dto.request.S3UploadURLRequestDTO;
import ru.nsu.crossfitbuddy.core.dto.response.S3UploadURLDTO;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.FileVisibility;
import ru.nsu.crossfitbuddy.core.entity.MimoType;
import ru.nsu.crossfitbuddy.core.entity.S3FileStatus;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.S3FileRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.repository.UserProfileRepository;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Testcontainers
class S3FileServiceIntegrationTest extends AbstractRepositoryTest {

  private static final String OLD_PASSWORD = "OldStrongPassword123";

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  UserAccountRepository accounts;

  @MockitoBean
  private MinioStorageService minioStorageService;

  @Autowired
  UserProfileRepository userProfileRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  private S3FilesService s3FilesService;

  @Autowired
  private S3FileRepository s3FileRepository;

  private UserAccount createTestAccount() {
    UserAccount account = UserAccount.builder()
        .email("testmail@test.com")
        .passwordHash(encoder.encode(OLD_PASSWORD))
        .roles(new HashSet<>(Set.of(UserRole.ATHLETE)))
        .activeRole(UserRole.ATHLETE)
        .enabled(true)
        .deleted(false)
        .build();
    var userProfile = UserProfile.create(account, "name", "name");
    userProfileRepository.saveAndFlush(userProfile);
    return account;
  }

  private CurrentUser createCurrentUser() {
    var account = createTestAccount();
    return new CurrentUser(account.getId(), account.getEmail(), "ATHLETE");
  }

  @Test
  @Transactional
  void Should_ReturnPendingLink_WhenRequestPending() throws Exception {

    var currentUser = createCurrentUser();
    var request = new S3UploadURLRequestDTO("name", MimoType.PNG, 123L);
    when(minioStorageService.pending(anyString())).thenReturn("url");

    S3UploadURLDTO response = s3FilesService.requestPending(request, FileVisibility.PUBLIC,
        currentUser);

    entityManager.flush();
    entityManager.clear();

    assertNotNull(response.fileId());
    assertNotNull(response.uploadUrl());
    assertNotNull(response.expiresAt());
    assertEquals("url", response.uploadUrl());
  }

  @Test
  @Transactional
  void Should_SaveEntityInDB_WhenRequestPending() throws Exception {

    var currentUser = createCurrentUser();
    var request = new S3UploadURLRequestDTO("name", MimoType.PNG, 123L);
    S3UploadURLDTO response = s3FilesService.requestPending(request, FileVisibility.PUBLIC,
        currentUser);

    entityManager.flush();
    entityManager.clear();

    var file = s3FileRepository.findById(response.fileId()).get();

    assertEquals(response.fileId(), file.getId());
    assertEquals(S3FileStatus.PENDING, file.getStatus());
    assertEquals(MimoType.PNG, file.getType());
    assertEquals(FileVisibility.PUBLIC, file.getVisibility());
  }

  @Test
  @Transactional
  void Should_SetFileIsReady_WhenSetReadyAndGet() throws Exception {

    var currentUser = createCurrentUser();
    var request = new S3UploadURLRequestDTO("name", MimoType.PNG, 123L);
    S3UploadURLDTO response = s3FilesService.requestPending(request, FileVisibility.PUBLIC,
        currentUser);
    when(minioStorageService.isObjectExist(anyString())).thenReturn(true);
    var file = s3FilesService.setFileReadyAndGet(response.fileId());
    entityManager.flush();
    entityManager.clear();

    var DBfile = s3FileRepository.findById(response.fileId()).get();

    assertEquals(S3FileStatus.READY, file.getStatus());
    assertEquals(S3FileStatus.READY, DBfile.getStatus());
    assertEquals(MimoType.PNG, file.getType());
    assertEquals(FileVisibility.PUBLIC, file.getVisibility());

  }

}
