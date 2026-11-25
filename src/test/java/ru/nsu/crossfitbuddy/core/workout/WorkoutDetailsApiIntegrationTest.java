package ru.nsu.crossfitbuddy.core.workout;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.nsu.crossfitbuddy.core.entity.*;
import ru.nsu.crossfitbuddy.core.repository.*;
import ru.nsu.crossfitbuddy.core.service.JwtService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.nsu.crossfitbuddy.core.entity.UserRole.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.jwt.secret=" + WorkoutDetailsApiIntegrationTest.SECRET
})
@Import(WorkoutFixtureConfiguration.class)
class WorkoutDetailsApiIntegrationTest extends AbstractRepositoryTest {
    static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    static final Instant DAY = Instant.parse("2026-11-09T00:00:00Z");

    @Autowired TestRestTemplate http;
    @LocalServerPort int port;
    @Autowired UserAccountRepository accounts;
    @Autowired WorkoutRepository workouts;
    @Autowired WorkoutParticipantRepository participants;
    @Autowired TrainerSubscriptionRepository subscriptions;
    @Autowired JwtService jwt;
    @Autowired WorkoutFixtureConfiguration.FixtureTime time;
    @Autowired PlatformTransactionManager txManager;
    @PersistenceContext EntityManager em;

    private final List<UUID> accountIds = new ArrayList<>();
    private final List<UUID> workoutIds = new ArrayList<>();
    private final List<UUID> participantIds = new ArrayList<>();
    private final List<UUID> subscriptionIds = new ArrayList<>();

    /**
     * Атлет запрашивает собственную карточку. В ответе есть название, длительность, дерево
     * complexes -> rounds -> exercises и материалы. Приватных полей (createdBy, participants,
     * type, updatedAt, workout внутри stage и т.д.) в JSON нет.
     */
    @DisplayName("Владелец получает карточку с комплексами, упражнениями и материалами без приватных полей")
    @Test
    void ownWorkoutReturnsFullCardWithoutPrivateFields() {
        // Given
        var athlete = account(ATHLETE);
        var workout = workout(athlete, WorkoutType.OWN,WorkoutStatus.READY, "AMRAP для всего тела", DAY);

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var amrap = new ComplexFormat(ComplexFormatType.AMRAP, "AMRAP", "AMRAP описание");
            var wod = WorkoutStage.builder().id(3).code("WOD").name("WOD").build();
            var burpee = new Exercise();
            burpee.setName("Приседания");
            burpee.setDescription("Описание приесдания");
            burpee.setCategory(ExerciseCategory.CARDIO);
            burpee.setCommon(true);
            em.persist(burpee);

            var complex = Complex.builder()
                .workout(workout).workoutStage(wod).complexFormat(amrap)
                .timeLimitSeconds(600).targetRounds(5).intervalSeconds(30)
                .build();
            em.persist(complex);

            var round = ComplexRound.builder()
                .complex(complex).position(1).repeatCount(3)
                .build();
            em.persist(round);

            var roundExercise = RoundExercise.builder()
                .round(round).exercise(burpee).position(1)
                .repetitions(10)
                .weightKg(new BigDecimal("60.00"))
                .notes("Без прыжка")
                .build();
            em.persist(roundExercise);

            em.persist(WorkoutMaterial.builder()
                .workout(workout)
                .workoutMaterialType(WorkoutMaterialType.VIDEO)
                .title("Демонстрация")
                .url("https://youtu.be/demo")
                .build());

            em.flush();
        });

        var token = token(athlete, ATHLETE);

        // When
        var response = getById(token, workout.getId());

        // Then (статус и заголовки)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var body = response.getBody();
        System.out.println(body.toPrettyString());

        // Then
        assertThat(body.path("id").asText()).isEqualTo(workout.getId().toString());
        assertThat(body.path("title").asText()).isEqualTo("AMRAP для всего тела");
        assertThat(body.path("status").asText()).isEqualTo("READY");
        assertThat(body.path("durationSec").asInt()).isEqualTo(600);

        // Then (complexes)
        assertThat(body.path("complexes")).hasSize(1);
        var stageJson = body.path("complexes").get(0);
        assertThat(stageJson.path("complexFormat").asText()).isEqualTo("AMRAP");
        assertThat(stageJson.path("workoutStage").asText()).isEqualTo("WOD");
        assertThat(stageJson.path("timeLimitSeconds").asInt()).isEqualTo(600);
        assertThat(stageJson.path("targetRounds").asInt()).isEqualTo(5);
        assertThat(stageJson.path("intervalSeconds").asInt()).isEqualTo(30);

        // Then (rounds)
        assertThat(stageJson.path("rounds")).hasSize(1);
        var roundJson = stageJson.path("rounds").get(0);
        assertThat(roundJson.path("position").asInt()).isEqualTo(1);
        assertThat(roundJson.path("repeatCount").asInt()).isEqualTo(3);

        // Then (exercises)
        assertThat(roundJson.path("exercises")).hasSize(1);
        var exerciseJson = roundJson.path("exercises").get(0);
        assertThat(exerciseJson.path("name").asText()).isEqualTo("Приседания");
        assertThat(exerciseJson.path("position").asInt()).isEqualTo(1);
        assertThat(exerciseJson.path("repetitions").asInt()).isEqualTo(10);
        assertThat(exerciseJson.path("weightKg").decimalValue())
            .isEqualByComparingTo("60.00");
        assertThat(exerciseJson.path("notes").asText()).isEqualTo("Без прыжка");

        // Then (materials)
        assertThat(body.path("materials")).hasSize(1);
        var materialJson = body.path("materials").get(0);
        assertThat(materialJson.path("type").asText()).isEqualTo("VIDEO");
        assertThat(materialJson.path("title").asText()).isEqualTo("Демонстрация");
        assertThat(materialJson.path("url").asText()).isEqualTo("https://youtu.be/demo");

        // Then (приватных полей нет)
        assertNoPrivateFields(body);

        Set<String> stageFields = new HashSet<>();
        stageJson.fieldNames().forEachRemaining(stageFields::add);
        assertThat(stageFields).doesNotContain("workout");

        Set<String> roundFields = new HashSet<>();
        roundJson.fieldNames().forEachRemaining(roundFields::add);
        assertThat(roundFields).doesNotContain("complex");

        Set<String> exerciseFields = new HashSet<>();
        exerciseJson.fieldNames().forEachRemaining(exerciseFields::add);
        assertThat(exerciseFields).doesNotContain("round", "exercise");
    }

    @DisplayName("TRAINER видит свою карточку")
    @Test
    void trainerSeesOwnWorkout() {
        var trainer = account(TRAINER);
        var own = workout(trainer, WorkoutType.OWN, WorkoutStatus.READY, "Своя", DAY);
        var response = getById(token(trainer, TRAINER), own.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @DisplayName("TRAINER видит назначенную тренировку своего атлета при ACTIVE-подписке")
    @Test
    void trainerSeesAssignedOfAthleteWithActiveSubscription() {
        var trainer = account(TRAINER);
        var athlete = account(ATHLETE);
        var assigned = workout(trainer, WorkoutType.ASSIGNED, WorkoutStatus.READY, "План", DAY);
        participant(assigned, trainer, athlete);
        subscribe(trainer, athlete, SubscriptionStatus.ACTIVE);

        var response = getById(token(trainer, TRAINER), assigned.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @DisplayName("Атлет видит назначенную карточку только при ACTIVE-подписке на тренера-автора")
    @ParameterizedTest
    @EnumSource(SubscriptionStatus.class)
    void assignedVisibleOnlyWithActiveSubscriptionToSameTrainer(SubscriptionStatus status) {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var assigned = workout(trainer, WorkoutType.ASSIGNED, WorkoutStatus.READY, "План", DAY);
        participant(assigned, trainer, athlete);
        subscribe(trainer, athlete, status);
        var token = token(athlete, ATHLETE);

        // When
        var response = getById(token, assigned.getId());

        // Then
        if (status == SubscriptionStatus.ACTIVE) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().path("id").asText())
                .isEqualTo(assigned.getId().toString());
            assertNoPrivateFields(response.getBody());
        } else {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().path("code").asText())
                .isEqualTo("WORKOUT_NOT_FOUND");
        }
    }

    @DisplayName("Подписка на другого тренера не открывает назначение")
    @Test
    void assignmentRequiresSubscriptionToTheSameTrainer() {
        // Given
        var athlete = account(ATHLETE);
        var authorTrainer = account(TRAINER);
        var otherTrainer = account(TRAINER);
        var assigned = workout(authorTrainer, WorkoutType.ASSIGNED, WorkoutStatus.READY, "План", DAY);
        participant(assigned, authorTrainer, athlete);
        subscribe(otherTrainer, athlete, SubscriptionStatus.ACTIVE);
        subscribe(authorTrainer, athlete, SubscriptionStatus.CANCELLED);

        // When
        var response = getById(token(athlete, ATHLETE), assigned.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("code").asText()).isEqualTo("WORKOUT_NOT_FOUND");
    }

    @DisplayName("Shared виден только участнику")
    @Test
    void sharedVisibleOnlyToParticipant() {
        // Given
        var participantAthlete = account(ATHLETE);
        var strangerAthlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var shared = workout(trainer, WorkoutType.SHARED, WorkoutStatus.READY, "Общая", DAY);
        participant(shared, null, participantAthlete);

        // When / Then
        var ok = getById(token(participantAthlete, ATHLETE), shared.getId());
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().path("id").asText()).isEqualTo(shared.getId().toString());
        assertNoPrivateFields(ok.getBody());

        var denied = getById(token(strangerAthlete, ATHLETE), shared.getId());
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(denied.getBody().path("code").asText()).isEqualTo("WORKOUT_NOT_FOUND");
    }

    @DisplayName("Своя карточка видна в DRAFT")
    @Test
    void ownDraftVisibleToOwner() {
        var athlete = account(ATHLETE);
        var own = workout(athlete, WorkoutType.OWN, WorkoutStatus.DRAFT, "Черновик", DAY);

        var response = getById(token(athlete, ATHLETE), own.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("id").asText()).isEqualTo(own.getId().toString());
        assertThat(response.getBody().path("status").asText()).isEqualTo("DRAFT");
    }

    @DisplayName("Черновики assigned и shared не видны атлету")
    @Test
    void draftAssignedAndSharedHiddenFromAthlete() {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var assignedDraft = workout(trainer, WorkoutType.ASSIGNED, WorkoutStatus.DRAFT, "Назначенный черновик", DAY);
        var sharedDraft = workout(trainer, WorkoutType.SHARED, WorkoutStatus.DRAFT, "Общий черновик", DAY);
        participant(assignedDraft, trainer, athlete);
        participant(sharedDraft, null, athlete);
        subscribe(trainer, athlete, SubscriptionStatus.ACTIVE);
        var token = token(athlete, ATHLETE);

        // When / Then
        assertThat(getById(token, assignedDraft.getId()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getById(token, sharedDraft.getId()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @DisplayName("Отсутствующая и недоступная карточки возвращают одинаковый ответ")
    @Test
    void missingAndInaccessibleReturnSameResponse() {
        // Given
        var athlete = account(ATHLETE);
        var foreignTrainer = account(TRAINER);
        var foreignWorkout = workout(foreignTrainer, WorkoutType.OWN, WorkoutStatus.READY, "Чужая", DAY);
        var token = token(athlete, ATHLETE);

        // When
        var missing = getById(token, UUID.randomUUID());
        var noAccess = getById(token, foreignWorkout.getId());

        // Then
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(noAccess.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(missing.getBody().path("code").asText())
            .isEqualTo("WORKOUT_NOT_FOUND");
        assertThat(noAccess.getBody().path("code").asText())
            .isEqualTo("WORKOUT_NOT_FOUND");
        assertThat(missing.getBody().path("message").asText())
            .isEqualTo(noAccess.getBody().path("message").asText());
    }
    @DisplayName("durationSec == сумма timeLimitSeconds всех стадий")
    @Test
    void durationIsSumOfAllStagesWhenAllHaveTimeLimit() {
        // Given
        var athlete = account(ATHLETE);
        var workout = workout(athlete, WorkoutType.OWN, WorkoutStatus.READY,"Две стадии", DAY);

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var amrap = new ComplexFormat(ComplexFormatType.AMRAP, "AMRAP", "AMRAP описание");
            var wod = WorkoutStage.builder().id(1).code("WOD").name("WOD").build();

            em.persist(Complex.builder()
                .workout(workout).workoutStage(wod).complexFormat(amrap)
                .timeLimitSeconds(300)
                .build());
            em.persist(Complex.builder()
                .workout(workout).workoutStage(wod).complexFormat(amrap)
                .timeLimitSeconds(200)
                .build());
            em.flush();
        });

        // When
        var response = getById(token(athlete, ATHLETE), workout.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body.has("durationSec")).isTrue();
        assertThat(body.path("durationSec").isNull()).isFalse();
        assertThat(body.path("durationSec").asInt()).isEqualTo(500);
        assertThat(body.path("complexes")).hasSize(2);
    }

    @DisplayName("durationSec = null, если хотя бы у одной стадии нет timeLimitSeconds")
    @Test
    void durationIsNullWhenAnyStageHasNoTimeLimit() {
        // Given
        var athlete = account(ATHLETE);
        var workout = workout(athlete, WorkoutType.OWN, WorkoutStatus.READY, "Частично с лимитом", DAY);

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var amrap = new ComplexFormat(ComplexFormatType.AMRAP, "AMRAP", "AMRAP описание");
            var wod = WorkoutStage.builder().id(1).code("WOD").name("WOD").build();

            em.persist(Complex.builder()
                .workout(workout).workoutStage(wod).complexFormat(amrap)
                .timeLimitSeconds(300)
                .build());
            em.persist(Complex.builder()
                .workout(workout).workoutStage(wod).complexFormat(amrap)
                .timeLimitSeconds(null)
                .build());
            em.flush();
        });

        // When
        var response = getById(token(athlete, ATHLETE), workout.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        var duration = body.path("durationSec");
        assertThat(duration.isMissingNode() || duration.isNull()).isTrue();
        assertThat(body.path("complexes")).hasSize(2);
        var nullLimit = false;
        for (var s : body.path("complexes")) {
            if (s.path("timeLimitSeconds").isNull() || s.path("timeLimitSeconds").isMissingNode()) {
                nullLimit = true;
            }
        }
        assertThat(nullLimit).isTrue();
    }

    @DisplayName("materials == пустой массив, если у тренировки нет материалов")
    @Test
    void materialsEmptyArrayWhenNonePresent() {
        // Given
        var athlete = account(ATHLETE);
        var workout = workout(athlete, WorkoutType.OWN, WorkoutStatus.READY, "Без материалов", DAY);

        // When
        var response = getById(token(athlete, ATHLETE), workout.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();

        assertThat(body.has("materials")).isTrue();
        assertThat(body.path("materials").isArray()).isTrue();
        assertThat(body.path("materials").isNull()).isFalse();
        assertThat(body.path("materials")).isEmpty();
    }

    @DisplayName("Без токена - 401")
    @Test
    void noTokenUnauthorized() {
        var athlete = account(ATHLETE);
        var own = workout(athlete, WorkoutType.OWN, WorkoutStatus.DRAFT, "Title", DAY);

        var response = getById(null, own.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UserAccount account(UserRole... roles) {
        var result = accounts.saveAndFlush(UserAccount.builder()
            .email(UUID.randomUUID() + "@workout.test")
            .passwordHash("unused").enabled(true)
            .roles(new HashSet<>(List.of(roles)))
            .activeRole(roles[0])
            .build());
        accountIds.add(result.getId());
        return result;
    }

    private Workout workout(UserAccount creator, WorkoutType type,
        WorkoutStatus status, String title, Instant date) {
        time.set(date);
        var result = workouts.saveAndFlush(Workout.builder()
            .createdBy(creator.getId())
            .type(type).status(status).title(title)
            .build());
        workoutIds.add(result.getId());
        return result;
    }

    private void assertNoPrivateFields(JsonNode body) {
        Set<String> top = new HashSet<>();
        body.fieldNames().forEachRemaining(top::add);
        assertThat(top).doesNotContain(
            "createdBy", "participants", "sharedWith", "updatedAt", "createdAt");
    }

    private WorkoutParticipant participant(Workout workout, UserAccount trainer, UserAccount athlete) {
        var result = participants.saveAndFlush(WorkoutParticipant.builder()
            .workout(workout).trainer(trainer).athlete(athlete).build());
        participantIds.add(result.getId());
        return result;
    }

    private TrainerSubscription subscribe(UserAccount trainer, UserAccount athlete, SubscriptionStatus status) {
        var result = subscriptions.saveAndFlush(TrainerSubscription.builder()
            .trainer(trainer).athlete(athlete).status(status).build());
        subscriptionIds.add(result.getId());
        return result;
    }

    private String token(UserAccount account, UserRole role) {
        return jwt.issue(account, role).value();
    }

    private ResponseEntity<JsonNode> getById(String token, UUID id) {
        var uri = UriComponentsBuilder
            .fromUriString("http://localhost:" + port + "/api/v1/workouts/" + id)
            .build().encode().toUri();
        var headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    @AfterEach
    void cleanup() {
        subscriptions.deleteAllByIdInBatch(subscriptionIds);
        participants.deleteAllByIdInBatch(participantIds);
        workouts.deleteAllByIdInBatch(workoutIds);
        accountIds.forEach(accounts::deleteById);
    }
}
