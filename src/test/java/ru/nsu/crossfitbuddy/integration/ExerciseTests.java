package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.tuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.EquipmentPropertiesRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExerciseEquipmentRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.entity.AccountDetails;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipmentProperties;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.mapper.GeneralExerciseMapper;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.GeneralExercisesService;

@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ExerciseTests extends AbstractRepositoryTest {
    @PersistenceContext
    private EntityManager entityManager;

    private static final String OLD_PASSWORD = "OldStrongPassword123";

    @Autowired
    UserAccountRepository accounts;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    GeneralExerciseMapper generalExerciseMapper;

    @Autowired
    GeneralExercisesRepository generalExercisesRepository;

    @Autowired
    private GeneralExercisesService generalExercisesService;

    private UserAccount createTestAccount() {
        UserAccount account = UserAccount.builder()
                .email("testmail@test.com")
                .passwordHash(encoder.encode(OLD_PASSWORD))
                .roles(new HashSet<>(Set.of(UserRole.ATHLETE)))
                .activeRole(UserRole.ATHLETE)
                .enabled(true)
                .deleted(false)
                .build();
        accounts.saveAndFlush(account);
        return account;
    }

    private void checkFieldsEquals(Exercise saved, GeneralExerciseRequest request) {
        assertThat(saved.getName()).isEqualTo(request.name());
        assertThat(saved.getDescription()).isEqualTo(request.description());
        assertThat(saved.getCategory()).isEqualTo(request.category());

        assertThat(saved.getImage()).isNull();
        assertThat(saved.getVideo()).isNull();

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
    void Should_DataEquals_WhenCreateNewExercise() throws Exception {
        var request = testData();

        var exercise = generalExercisesService.createExercise(request,
                new AccountDetails(createTestAccount()));
        generalExercisesRepository.flush();

        GeneralExerciseRequest updateRequest = updateTestData(exercise.getId(),
                exercise.getExerciseEquipments().getFirst().getId(),
                exercise.getExercisesMetrics().getFirst().getId());

        generalExercisesService.updateExercises(updateRequest);
        generalExercisesRepository.flush();

        var saved = generalExercisesRepository.findById(exercise.getId()).get();

        checkFieldsEquals(saved, updateRequest);

    }

    @Test
    @Transactional
    void Should_UpdateCorrect_WhenUpdateExercise() throws Exception {
        var request = testData();

        var exercise = generalExercisesService.createExercise(request,
                new AccountDetails(createTestAccount()));
        generalExercisesRepository.flush();

        var saved = generalExercisesRepository.findById(exercise.getId()).get();
        checkFieldsEquals(saved, request);

    }

    private GeneralExerciseRequest testData() {
        return new GeneralExerciseRequest(
                null, // id
                "Приседания",
                "Упражнение для тренировки мышц ног и ягодиц",
                List.of(
                        new ExercisesMetricsRequest(
                                null, // id
                                "Количество повторений",
                                MetricsValueType.INTEGER,
                                "повторений"),
                        new ExercisesMetricsRequest(
                                null, // id
                                "Вес",
                                MetricsValueType.INTEGER,
                                "кг")),
                List.of(
                        new ExerciseEquipmentRequest(
                                null, // id
                                "Штанга",
                                null, // imageFileId
                                List.of(
                                        new EquipmentPropertiesRequest(
                                                "Вес",
                                                MetricsValueType.FLOAT,
                                                "кг")))),
                ExerciseCategory.STRENGTH,
                null, // imageFileId
                null // videoFileId
        );
    }

    private GeneralExerciseRequest updateTestData(UUID exId, UUID eqId, UUID m1Id) {
        return new GeneralExerciseRequest(
                exId, // id
                "Отжимания",
                "ОтжиманияОтжимания",
                List.of(
                        new ExercisesMetricsRequest(
                                m1Id, // id
                                "Количество повторений",
                                MetricsValueType.INTEGER,
                                "повторений")),
                List.of(
                        new ExerciseEquipmentRequest(
                                eqId, // id
                                "Штанга update",
                                null, // imageFileId
                                List.of(
                                        new EquipmentPropertiesRequest(
                                                "Весupdate",
                                                MetricsValueType.FLOAT,
                                                "кг")))),
                ExerciseCategory.STRENGTH,
                null, // imageFileId
                null // videoFileId
        );
    }

}
