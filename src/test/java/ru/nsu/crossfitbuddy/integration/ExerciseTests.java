package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import lombok.extern.slf4j.Slf4j;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.EquipmentPropertiesRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.EquipmentPropertiesRequest.EquipmentPropertiesRequestBuilder;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExerciseEquipmentRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExerciseEquipmentRequest.ExerciseEquipmentRequestBuilder;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest.ExercisesMetricsRequestBuilder;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest.GeneralExerciseRequestBuilder;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipmentProperties;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.FileVisibility;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;
import ru.nsu.crossfitbuddy.core.entity.MimoType;
import ru.nsu.crossfitbuddy.core.entity.S3File;
import ru.nsu.crossfitbuddy.core.entity.S3FileStatus;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.mapper.GeneralExerciseMapper;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;
import ru.nsu.crossfitbuddy.core.repository.S3FileRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.repository.UserProfileRepository;
import ru.nsu.crossfitbuddy.core.service.GeneralExercisesService;
import ru.nsu.crossfitbuddy.core.service.S3FilesService;

@Slf4j
@SpringBootTest(properties = {
    "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Testcontainers
class ExerciseTests extends AbstractRepositoryTest {
  @PersistenceContext
  private EntityManager entityManager;

  private static final String OLD_PASSWORD = "OldStrongPassword123";

  @Autowired
  UserAccountRepository accounts;

  @Autowired
  PasswordEncoder encoder;

  @MockitoBean
  S3FilesService s3FilesService;

  @Autowired
  GeneralExerciseMapper generalExerciseMapper;

  @Autowired
  GeneralExercisesRepository generalExercisesRepository;

  @Autowired
  S3FileRepository s3FileRepository;

  @Autowired
  UserProfileRepository userProfileRepository;

  @Autowired
  private GeneralExercisesService generalExercisesService;

  private UUID fileId1 = UUID.randomUUID();
  private UUID fileId2 = UUID.randomUUID();

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

  private void checkFieldsEquals(Exercise saved, GeneralExerciseRequest request) {
    assertThat(saved.getName()).isEqualTo(request.name());
    assertThat(saved.getDescription()).isEqualTo(request.description());
    assertThat(saved.getCategory()).isEqualTo(request.category());

    assertThat(saved.getImage()).matches(image -> (image == null && request.imageFileId() == null)
        || (image.getId().equals(request.imageFileId())), "img id must be equals");
    assertThat(saved.getVideo()).matches(video -> (video == null && request.videoFileId() == null)
        || (video.getId().equals(request.videoFileId())));

    assertThat(saved.getExercisesMetrics()).hasSize(request.exercisesMetrics().size());
    assertThat(saved.getExercisesMetrics())
        .extracting(
            ExercisesMetrics::getName,
            ExercisesMetrics::getMetricsValueType,
            ExercisesMetrics::getUnit)
        .containsExactlyElementsOf(
            request.exercisesMetrics().stream()
                .map(m -> tuple(m.name(), m.metricsValueType(), m.unit()))
                .toList());

    // Оборудование: name и imageFileId
    assertThat(saved.getExerciseEquipments())
        .extracting(
            ExerciseEquipment::getName,
            equipment -> equipment.getImage() == null
                ? null
                : equipment.getImage().getId())
        .containsExactlyElementsOf(
            request.exercisesEquipments().stream()
                .map(e -> tuple(e.name(), e.imageFileId()))
                .toList());

    // Свойства оборудования: name, metricsValueType, unit
    for (int i = 0; i < request.exercisesEquipments().size(); i++) {
      var expectedEquipment = request.exercisesEquipments().get(i);
      var savedEquipment = saved.getExerciseEquipments().get(i);

      assertThat(savedEquipment.getEquipmentProperties())
          .extracting(
              ExerciseEquipmentProperties::getName,
              ExerciseEquipmentProperties::getMetricsValueType,
              ExerciseEquipmentProperties::getUnit)
          .containsExactlyElementsOf(
              expectedEquipment.propertiesRequests().stream()
                  .map(p -> tuple(p.name(), p.metricsValueType(), p.unit()))
                  .toList());
    }
  }

  @Test
  @Transactional
  void Should_UpdateCorrect_WhenUpdateExercise() throws Exception {
    var request = testData();
    var account = createTestAccount();
    var exercise = generalExercisesService.createExercise(request,
        new CurrentUser(account.getId(), account.getEmail(), "ATHLETE"));

    var updateRequest = generalExercise()
        .id(exercise.getId())
        .clearExercisesEquipments()
        .exercisesEquipment(
            defaultBarbell().id(exercise.getExerciseEquipments().getFirst().getId()).build())
        .clearExercisesMetrics()
        .exercisesMetric(
            repsMetric().id(exercise.getExercisesMetrics().getFirst().getId()).build())
        .build();

    generalExercisesService.updateExercises(updateRequest);
    entityManager.flush();
    entityManager.clear();

    var saved = generalExercisesRepository.findById(exercise.getId()).get();
    checkFieldsEquals(saved, updateRequest);

  }

  @Test
  @Transactional
  void Should_DataEquals_WhenCreateNewExercise() throws Exception {

    var request = testData();
    var account = createTestAccount();
    var exercise = generalExercisesService.createExercise(request,
        new CurrentUser(account.getId(), account.getEmail(), "ATHLETE"));

    entityManager.flush();
    entityManager.clear();

    var saved = generalExercisesRepository.findById(exercise.getId()).get();
    checkFieldsEquals(saved, request);

  }

  @Test
  @Transactional
  void Should_createExerciseWithImg_WhenCreateExercise() throws Exception {

    var request = generalExercise().imageFileId(fileId1).videoFileId(fileId1)
        .clearExercisesEquipments()
        .exercisesEquipment(defaultBarbell().imageFileId(fileId1).build())
        .build();

    var account = createTestAccount();
    var file = new S3File(fileId1, "name", MimoType.PNG, 1024L, "key",
        S3FileStatus.READY,
        Instant.now(), FileVisibility.PUBLIC, account);

    when(s3FilesService.setFileReadyAndGet(fileId1)).thenReturn(file);

    var exercise = generalExercisesService.createExercise(request,
        new CurrentUser(account.getId(), account.getEmail(), "ATHLETE"));
    generalExercisesRepository.flush();

    var saved = generalExercisesRepository.findById(exercise.getId()).get();
    checkFieldsEquals(saved, request);

  }

  @Test
  @Transactional
  void Should_UpdateImg_WhenUpdateRequestCaled() throws Exception {
    var request = testDataWithImg(fileId1);
    var account = createTestAccount();
    var file = new S3File(fileId1, "name", MimoType.PNG, 1024L, "key",
        S3FileStatus.READY,
        Instant.now(), FileVisibility.PUBLIC, account);

    when(s3FilesService.setFileReadyAndGet(fileId1)).thenReturn(file);

    var exercise = generalExercisesService.createExercise(request,
        new CurrentUser(account.getId(), account.getEmail(), "ATHLETE"));

    var newFile = new S3File(null, "name2", MimoType.PNG, 1024L, "key2",
        S3FileStatus.READY,
        Instant.now(), FileVisibility.PUBLIC, account);
    newFile = s3FileRepository.save(newFile);

    when(s3FilesService.setFileReadyAndGet(newFile.getId())).thenReturn(newFile);

    var newRequest = generalExercise()
        .name("Новое имя не приссе")
        .id(exercise.getId())
        .imageFileId(newFile.getId())
        .videoFileId(newFile.getId())
        .clearExercisesEquipments()
        .exercisesEquipment(defaultBarbell().id(exercise.getExerciseEquipments().getFirst().getId())
            .imageFileId(newFile.getId()).build())
        .build();

    generalExercisesService.updateExercises(newRequest);

    entityManager.flush();
    entityManager.clear();

    var saved = generalExercisesRepository.findById(exercise.getId()).get();

    checkFieldsEquals(saved, newRequest);
  }

  @Test
  @Transactional
  void Should_AddExistEquipment_WhenAddcreateNewExercise() {
    var account = createTestAccount();
    var exercise = saveBaseExercise(account);
    var equipment = exercise.getExerciseEquipments().getFirst();

    var request = generalExercise()
        .clearExercisesEquipments()
        .exercisesEquipment(
            defaultBarbell().id(equipment.getId()).build())
        .build();

    var newExercise = saveExercise(request, account);

    var saved = generalExercisesRepository.findById(newExercise.getId()).get();

    assertEquals(request.exercisesEquipments().getFirst().id(),
        saved.getExerciseEquipments().getFirst().getId());
  }

  @Test
  @Transactional
  void Should_AddExistMetric_WhenAddcreateNewExercise() {
    var account = createTestAccount();
    var exercise = saveBaseExercise(account);
    var metric = exercise.getExercisesMetrics().getFirst();

    var request = generalExercise()
        .clearExercisesMetrics()
        .exercisesMetric(
            repsMetric().id(metric.getId()).build())
        .build();

    var newExercise = saveExercise(request, account);

    var saved = generalExercisesRepository.findById(newExercise.getId()).get();

    assertEquals(request.exercisesMetrics().getFirst().id(),
        saved.getExercisesMetrics().getFirst().getId());
  }

  private Exercise saveBaseExercise(UserAccount account) {
    var request = testData();
    return saveExercise(request, account);
  }

  private Exercise saveExercise(GeneralExerciseRequest request, UserAccount account) {
    var exercise = generalExercisesService.createExercise(request,
        new CurrentUser(account.getId(), account.getEmail(), "ATHLETE"));

    entityManager.flush();
    entityManager.clear();

    return exercise;
  }

  @Test
  @Transactional
  void Should_SaveExercise_WhenAddExerciseWithouMetrics() {
    var account = createTestAccount();
    var request = generalExercise().clearExercisesMetrics().build();

    var exercise = saveExercise(request, account);

    var saved = generalExercisesRepository.findById(exercise.getId()).get();

    checkFieldsEquals(saved, request);
  }

  @Test
  @Transactional
  void Should_SaveExercise_WhenAddExerciseWithouEquipments() {
    var account = createTestAccount();
    var request = generalExercise().clearExercisesEquipments().build();

    var exercise = saveExercise(request, account);

    var saved = generalExercisesRepository.findById(exercise.getId()).get();

    checkFieldsEquals(saved, request);
  }

  private GeneralExerciseRequest testDataWithImg(UUID id) {
    return generalExercise().imageFileId(id).videoFileId(id)
        .clearExercisesEquipments()
        .exercisesEquipment(defaultBarbell().imageFileId(id).build())
        .build();
  }

  private EquipmentPropertiesRequestBuilder defaultWeightProperty() {
    return EquipmentPropertiesRequest.builder()
        .name("Вес")
        .metricsValueType(MetricsValueType.FLOAT)
        .unit("кг");
  }

  private ExerciseEquipmentRequestBuilder defaultBarbell() {
    return ExerciseEquipmentRequest.builder()
        .name("Штанга")
        .imageFileId(null)
        .propertiesRequest(defaultWeightProperty().build());
  }

  private ExercisesMetricsRequestBuilder repsMetric() {
    return ExercisesMetricsRequest.builder()
        .name("Количество повторений")
        .metricsValueType(MetricsValueType.INTEGER)
        .unit("повторений");
  }

  private ExercisesMetricsRequestBuilder weightMetric() {
    return ExercisesMetricsRequest.builder()
        .name("Вес")
        .metricsValueType(MetricsValueType.INTEGER)
        .unit("кг");
  }

  private GeneralExerciseRequestBuilder generalExercise() {
    return GeneralExerciseRequest.builder()
        .name("Приседания")
        .description("Упражнение для тренировки мышц ног и ягодиц")
        .category(ExerciseCategory.STRENGTH)
        .exercisesMetric(repsMetric().build())
        .exercisesMetric(weightMetric().build())
        .exercisesEquipment(defaultBarbell().build());
  }

  private GeneralExerciseRequest testData() {
    return generalExercise().build();
  }

}
