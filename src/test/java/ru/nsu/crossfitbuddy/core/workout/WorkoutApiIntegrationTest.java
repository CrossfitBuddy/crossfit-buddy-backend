package ru.nsu.crossfitbuddy.core.workout;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import ru.nsu.crossfitbuddy.core.entity.*;
import ru.nsu.crossfitbuddy.core.repository.*;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.nsu.crossfitbuddy.core.entity.UserRole.*;
import static ru.nsu.crossfitbuddy.core.entity.WorkoutStatus.*;
import static ru.nsu.crossfitbuddy.core.entity.WorkoutType.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.jwt.secret=" + WorkoutApiIntegrationTest.SECRET,
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@Import(WorkoutFixtureConfiguration.class)
class WorkoutApiIntegrationTest extends AbstractRepositoryTest {
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
    @Autowired EntityManagerFactory factory;
    private final List<UUID> accountIds = new ArrayList<>();
    private final List<UUID> workoutIds = new ArrayList<>();
    private final List<UUID> participantIds = new ArrayList<>();
    private final List<UUID> subscriptionIds = new ArrayList<>();

    /**
     * При активной подписке ответ содержит три доступные тренировки с источниками OWN, ASSIGNED и SHARED.
     */
    @DisplayName("Атлет получает свои, назначенные и предоставленные ему тренировки")
    @Test
    void returnsOwnAssignedAndExplicitlySharedWithRelativeSources() {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var foreignAthlete = account(ATHLETE);
        var ownDraft = workout(
            athlete,
            ASSIGNED,
            DRAFT,
            "Моя",
            DAY.plusSeconds(3)
        );
        var assigned = workout(
            trainer,
            ASSIGNED,
            READY,
            "Назначенная",
            DAY.plusSeconds(2)
        );
        var shared = workout(
            trainer,
            SHARED,
            COMPLETED,
            "Общая",
            DAY.plusSeconds(1)
        );
        participant(
            assigned,
            trainer,
            athlete
        );
        participant(
            shared,
            null,
            athlete
        );
        participant(
            shared,
            null,
            athlete
        );
        subscribe(
            trainer,
            athlete,
            SubscriptionStatus.ACTIVE
        );
        participant(
            workout(
                trainer,
                OWN,
                READY,
                "Чужая own",
                DAY
            ),
            trainer,
            athlete
        );
        participant(
            workout(
                trainer,
                SHARED,
                DRAFT,
                "Черновик",
                DAY
            ),
            null,
            athlete
        );
        participant(
            workout(
                trainer,
                ASSIGNED,
                DRAFT,
                "Назначенный черновик",
                DAY
            ),
            trainer,
            athlete
        );
        workout(
            trainer,
            SHARED,
            READY,
            "Не опубликована всем",
            DAY
        );
        participant(
            workout(
                trainer,
                SHARED,
                READY,
                "Другому атлету",
                DAY
            ),
            null,
            foreignAthlete
        );
        String token = token(athlete, ATHLETE);

        // When
        var response = get(token, params());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        var body = response.getBody();
        assertThat(ids(body)).containsExactly(
            ownDraft.getId().toString(),
            assigned.getId().toString(),
            shared.getId().toString()
        );
        assertThat(body.path("totalElements").asInt()).isEqualTo(3);
        assertThat(body.path("items").get(0).path("source").asText()).isEqualTo("OWN");
        assertThat(body.path("items").get(1).path("source").asText()).isEqualTo("ASSIGNED");
        assertThat(body.path("items").get(2).path("source").asText()).isEqualTo("SHARED");
        Set<String> fields = new HashSet<>();
        body.path("items").get(0).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder(
            "id",
            "title",
            "date",
            "status",
            "source"
        );
    }

    /**
     * Для каждого статуса подписки атлет запрашивает ASSIGNED. В ответ попадает только назначение с
     * совпадающей парой атлет–тренер и подпиской ACTIVE.
     */
    @DisplayName("Назначение видно только при активной подписке на тренера из назначения")
    @ParameterizedTest
    @EnumSource(SubscriptionStatus.class)
    void checksSubscriptionForTheExactAssignmentPair(SubscriptionStatus status) {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var author = account(TRAINER);
        var otherAthlete = account(ATHLETE);
        var assigned = workout(
            author,
            ASSIGNED,
            READY,
            "План",
            DAY
        );
        participant(
            assigned,
            trainer,
            athlete
        );
        subscribe(
            trainer,
            athlete,
            status
        );
        subscribe(
            author,
            athlete,
            SubscriptionStatus.ACTIVE
        );
        subscribe(
            trainer,
            otherAthlete,
            SubscriptionStatus.ACTIVE
        );
        var noAssignment = workout(
            author,
            ASSIGNED,
            READY,
            "Нет назначения",
            DAY
        );
        participant(
            noAssignment,
            trainer,
            otherAthlete
        );
        var noTrainer = workout(
            author,
            ASSIGNED,
            READY,
            "Нет тренера",
            DAY
        );
        participant(
            noTrainer,
            null,
            athlete
        );
        String token = token(athlete, ATHLETE);

        // When
        var response = get(token, params("source", "ASSIGNED"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactlyElementsOf(status == SubscriptionStatus.ACTIVE
            ? List.of(assigned.getId().toString()) : List.of());
    }

    /**
     * Достаточно одного назначения, для которого подписка активна. И в списке, и в общем количестве
     * тренировка учитывается один раз.
     */
    @DisplayName("При нескольких назначениях доступная тренировка возвращается один раз")
    @Test
    void usesAnyMatchingActiveAssignmentWithoutMultiplyingResults() {
        // Given
        var athlete = account(ATHLETE);
        var firstTrainer = account(TRAINER);
        var secondTrainer = account(TRAINER);
        var assigned = workout(
            firstTrainer,
            ASSIGNED,
            READY,
            "План",
            DAY
        );
        participant(
            assigned,
            firstTrainer,
            athlete
        );
        participant(
            assigned,
            secondTrainer,
            athlete
        );
        subscribe(
            firstTrainer,
            athlete,
            SubscriptionStatus.CANCELLED
        );
        subscribe(
            secondTrainer,
            athlete,
            SubscriptionStatus.ACTIVE
        );

        // When
        var response = get(token(athlete, ATHLETE), params("size", "1"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(assigned.getId().toString());
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(1);
    }

    /**
     * Подписка на тренера отменена. Запрос атлета возвращает только SHARED, а
     * общее количество равно единице.
     */
    @DisplayName("После отмены подписки из двух тренировок остаётся предоставленная")
    @Test
    void revokesAssignedButKeepsSharedAfterSubscriptionCancellation() {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var assigned = workout(
            trainer,
            ASSIGNED,
            READY,
            "План",
            DAY
        );
        var shared = workout(
            trainer,
            SHARED,
            READY,
            "Общая",
            DAY
        );
        participant(
            assigned,
            trainer,
            athlete
        );
        participant(
            shared,
            trainer,
            athlete
        );
        var subscription = subscribe(
            trainer,
            athlete,
            SubscriptionStatus.ACTIVE
        );
        String token = token(athlete, ATHLETE);
        subscription.changeStatus(SubscriptionStatus.CANCELLED);
        subscriptions.saveAndFlush(subscription);

        // When
        var response = get(token, params());

        // Then
        assertThat(ids(response.getBody())).containsExactly(shared.getId().toString());
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(1);
    }

    /**
     * При активной подписке удаляется связь атлета с назначенной тренировкой. Следующий запрос возвращает
     * пустой список и нулевое количество совпадений.
     */
    @DisplayName("После удаления назначения список тренировок атлета пуст")
    @Test
    void revokesAccessWhenAssignmentIsDeleted() {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var assigned = workout(
            trainer,
            ASSIGNED,
            READY,
            "План",
            DAY
        );
        var participation = participant(
            assigned,
            trainer,
            athlete
        );
        subscribe(
            trainer,
            athlete,
            SubscriptionStatus.ACTIVE
        );
        participants.deleteById(participation.getId());

        // When
        var response = get(token(athlete, ATHLETE), params());

        // Then
        assertThat(ids(response.getBody())).isEmpty();
        assertThat(response.getBody().path("totalElements").asInt()).isZero();
    }

    /**
     * Атлет задаёт слова из названия, источники, статусы и обе границы периода на 9 ноября 2026 года.
     * Ответ содержит две подходящие тренировки в начале и конце дня по UTC, сначала более позднюю.
     */
    @DisplayName("Поиск с фильтрами за один день возвращает оба граничных совпадения")
    @Test
    void combinesKeywordsAndAllFiltersWithInclusiveUtcDay() {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        subscribe(
            trainer,
            athlete,
            SubscriptionStatus.ACTIVE
        );
        var start = workout(
            trainer,
            ASSIGNED,
            READY,
            "Сила НОГ",
            DAY
        );
        var end = workout(
            trainer,
            ASSIGNED,
            COMPLETED,
            "Ног и сила",
            DAY.plusSeconds(86400).minusNanos(1000)
        );
        participant(
            start,
            trainer,
            athlete
        );
        participant(
            end,
            trainer,
            athlete
        );
        participant(
            workout(
                trainer,
                ASSIGNED,
                READY,
                "Сила ног завтра",
                DAY.plusSeconds(86400)
            ),
            trainer,
            athlete
        );
        participant(
            workout(
                trainer,
                ASSIGNED,
                READY,
                "Сила ног вчера",
                DAY.minusNanos(1000)
            ),
            trainer,
            athlete
        );
        participant(
            workout(
                trainer,
                ASSIGNED,
                CANCELLED,
                "Сила ног отмена",
                DAY
            ),
            trainer,
            athlete
        );
        participant(
            workout(
                trainer,
                ASSIGNED,
                READY,
                "Сила рук",
                DAY
            ),
            trainer,
            athlete
        );
        workout(
            athlete,
            OWN,
            READY,
            "Сила ног личная",
            DAY
        );
        var filters = params(
            "q",
            "  нОг\tсила  ",
            "source",
            "ASSIGNED",
            "status",
            "READY",
            "status",
            "COMPLETED",
            "dateFrom",
            "2026-11-09",
            "dateTo",
            "2026-11-09"
        );

        // When
        var response = get(token(athlete, ATHLETE), filters);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(end.getId().toString(), start.getId().toString());
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(2);
    }

    /**
     * В поиске по названию передаются символ процента, подчёркивание, восклицательный знак или строка,
     * похожая на SQL. В каждом случае возвращается только тренировка с соответствующим текстом.
     */
    @DisplayName("Поиск по спецсимволам возвращает только буквальное совпадение")
    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!", "' OR 1=1 --"})
    void treatsSearchMetacharactersAsLiteralText(String keyword) {
        // Given
        var athlete = account(ATHLETE);
        var match = workout(
            athlete,
            OWN,
            READY,
            "Текст " + keyword,
            DAY
        );
        workout(
            athlete,
            OWN,
            READY,
            "Обычное название",
            DAY
        );
        workout(
            athlete,
            OWN,
            READY,
            null,
            DAY
        );

        // When
        var response = get(token(athlete, ATHLETE), params("q", keyword));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(match.getId().toString());
    }

    /**
     * Чужие тренировки подходят под фильтры. При каждой сортировке в странице и счётчиках учитывается только своя.
     */
    @DisplayName("Добавление недоступных тренировок сохраняет состав страницы и счётчики")
    @ParameterizedTest
    @ValueSource(strings = {"DATE_ASC", "DATE_DESC", "TITLE_ASC", "TITLE_DESC"})
    void inaccessibleRecordsNeverChangeFilteredResultsOrCounts(String sort) {
        // Given
        var athlete = account(ATHLETE);
        var foreign = account(TRAINER);
        var visible = workout(
            athlete,
            OWN,
            READY,
            "Сила ног",
            DAY
        );
        var filter = params(
            "q",
            "сила",
            "status",
            "READY",
            "source",
            "OWN",
            "source",
            "SHARED",
            "source",
            "ASSIGNED",
            "dateFrom",
            "2026-11-09",
            "dateTo",
            "2026-11-09",
            "sort",
            sort,
            "size",
            "1"
        );
        String token = token(athlete, ATHLETE);
        for (WorkoutType type : WorkoutType.values()) {
            workout(
                foreign,
                type,
                READY,
                "ААА сила ног",
                DAY.plusSeconds(1)
            );
            workout(
                foreign,
                type,
                READY,
                "ЯЯЯ сила ног",
                DAY.plusSeconds(2)
            );
        }

        // When
        var response = get(token, filter);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(visible.getId().toString());
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(1);
        assertThat(response.getBody().path("totalPages").asInt()).isEqualTo(1);
    }

    /**
     * Тренер создал тренировку без доступа для атлета. Для обоих поисковых запросов счётчики равны нулю.
     */
    @DisplayName("Поиск по скрытому или отсутствующему названию возвращает пустую страницу")
    @ParameterizedTest
    @ValueSource(strings = {"секретноеимя", "несуществующееимя"})
    void searchWithoutAccessibleMatchesReturnsEmptyPage(String keyword) {
        // Given
        var athlete = account(ATHLETE);
        workout(
            account(TRAINER),
            SHARED,
            READY,
            "секретноеимя",
            DAY
        );

        // When
        var response = get(token(athlete, ATHLETE), params("q", keyword));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).isEmpty();
        assertThat(response.getBody().path("totalElements").asInt()).isZero();
        assertThat(response.getBody().path("totalPages").asInt()).isZero();
    }

    /**
     * Четыре тренировки запрашиваются страницами по две с сортировкой по дате или названию. Проверяются
     * порядок при равных значениях, название null, счётчики и пустая страница за пределами результата.
     */
    @DisplayName("При совпадающих ключах сортировки страницы сохраняют заданный порядок")
    @ParameterizedTest
    @CsvSource({
        "DATE_ASC, 0", "DATE_ASC, 1", "DATE_ASC, 2",
        "DATE_DESC, 0", "DATE_DESC, 1", "DATE_DESC, 2",
        "TITLE_ASC, 0", "TITLE_ASC, 1", "TITLE_ASC, 2",
        "TITLE_DESC, 0", "TITLE_DESC, 1", "TITLE_DESC, 2"
    })
    void sortsAndPaginatesBeforeReturningWithStableTiesAndNullTitles(String sort, int page) {
        // Given
        var athlete = account(ATHLETE);
        var alpha = workout(
            athlete,
            OWN,
            READY,
            "alpha",
            DAY.plusSeconds(1)
        );
        var bravo = workout(
            athlete,
            OWN,
            READY,
            "Bravo",
            DAY.plusSeconds(2)
        );
        var bravoTie = workout(
            athlete,
            OWN,
            READY,
            "bravo",
            DAY.plusSeconds(2)
        );
        var unnamed = workout(
            athlete,
            OWN,
            READY,
            null,
            DAY.plusSeconds(3)
        );
        var tied = new ArrayList<>(List.of(bravo.getId().toString(), bravoTie.getId().toString()));
        tied.sort(Comparator.reverseOrder());
        var expected = new ArrayList<String>();
        if (sort.equals(WorkoutSortingKey.DATE_DESC.name())) expected.add(unnamed.getId().toString());
        if (sort.endsWith("ASC")) expected.add(alpha.getId().toString());
        expected.addAll(tied);
        if (sort.endsWith("DESC")) expected.add(alpha.getId().toString());
        if (!sort.equals(WorkoutSortingKey.DATE_DESC.name())) expected.add(unnamed.getId().toString());
        String token = token(athlete, ATHLETE);

        int start = Math.min(page * 2, expected.size());
        int end = Math.min(start + 2, expected.size());

        // When
        var response = get(token, params(
            "sort",
            sort,
            "page",
            Integer.toString(page),
            "size",
            "2",
            "q",
            "   "
        ));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactlyElementsOf(expected.subList(start, end));
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(4);
        assertThat(response.getBody().path("totalPages").asInt()).isEqualTo(2);
        assertThat(response.getBody().path("size").asInt()).isEqualTo(2);
        for (var item : response.getBody().path("items")) {
            if (item.path("id").asText().equals(unnamed.getId().toString())) {
                assertThat(item.path("title").isNull()).isTrue();
            }
        }
    }

    /**
     * У пользователя обе роли. После выбора TRAINER в профиле токен каждой из двух ролей открывает пустую страницу.
     */
    @DisplayName("JWT с ролью атлета или тренера открывает список")
    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ATHLETE", "TRAINER"})
    void acceptsSupportedJwtRole(UserRole role) {
        // Given
        var dual = account(ATHLETE, TRAINER);
        String bearer = token(dual, role);
        dual.setActiveRole(TRAINER);
        accounts.saveAndFlush(dual);

        // When
        var response = get(bearer, params());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("totalPages").asInt()).isZero();
    }

    /**
     * Действующий JWT с активной ролью ADMIN приводит к ошибке ACCESS_DENIED.
     */
    @DisplayName("Запрос администратора возвращает 403")
    @Test
    void rejectsAdministrator() {
        // Given
        var admin = account(ADMIN);

        // When
        var response = get(token(admin, ADMIN), params());

        // Then
        assertError(
            response,
            403,
            "ACCESS_DENIED"
        );
    }

    /**
     * Аккаунт совмещает роли. Поиск, статус, источники и даты применяются с учётом активной роли из JWT.
     */
    @DisplayName("Тренер видит одну собственную при совместных фильтрах")
    @ParameterizedTest
    @ValueSource(strings = {"DATE_ASC", "DATE_DESC", "TITLE_ASC", "TITLE_DESC"})
    void filtersWorkoutsForTrainer(String sort) {
        // Given
        var data = createRoleWorkouts();
        var filters = params(
            "q",
            "сила ног",
            "status",
            "READY",
            "source",
            "OWN",
            "source",
            "ASSIGNED",
            "source",
            "SHARED",
            "dateFrom",
            "2026-11-09",
            "dateTo",
            "2026-11-09",
            "sort",
            sort,
            "size",
            "1"
        );

        // When
        var response = get(token(data.user(), TRAINER), filters);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(1);
        assertThat(ids(response.getBody())).containsExactly(data.own().getId().toString());
        assertThat(response.getBody().path("items").get(0).path("source").asText()).isEqualTo("OWN");
    }

    /**
     * Аккаунт совмещает роли. Поиск, статус, источники и даты применяются с учётом активной роли из JWT.
     */
    @DisplayName("Атлет видит три доступные при совместных фильтрах")
    @ParameterizedTest
    @ValueSource(strings = {"DATE_ASC", "DATE_DESC", "TITLE_ASC", "TITLE_DESC"})
    void filtersWorkoutsForAthlete(String sort) {
        // Given
        var data = createRoleWorkouts();
        var filters = params(
            "q",
            "сила ног",
            "status",
            "READY",
            "source",
            "OWN",
            "source",
            "ASSIGNED",
            "source",
            "SHARED",
            "dateFrom",
            "2026-11-09",
            "dateTo",
            "2026-11-09",
            "sort",
            sort,
            "size",
            "1"
        );

        // When
        var response = get(token(data.user(), ATHLETE), filters);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(3);
    }

    /**
     * У тренера есть собственные тренировки и доступ к чужим как у атлета. В роли тренера видны только собственные.
     */
    @DisplayName("Без фильтров тренер получает все три свои тренировки")
    @Test
    void trainerListsAllOwn() {
        // Given
        var data = createRoleWorkouts();

        // When
        var response = get(token(data.user(), TRAINER), params());

        // Then
        assertThat(ids(response.getBody())).containsExactlyInAnyOrder(
            data.own().getId().toString(),
            data.draft().getId().toString(),
            data.other().getId().toString()
        );
    }

    /**
     * Тренировки этих источников доступны аккаунту в роли атлета. Для роли TRAINER счётчик совпадений равен нулю.
     */
    @DisplayName("Фильтр ASSIGNED или SHARED возвращает тренеру пустую страницу")
    @ParameterizedTest
    @ValueSource(strings = {"ASSIGNED", "SHARED"})
    void trainerFiltersForeignSources(String source) {
        // Given
        var data = createRoleWorkouts();

        // When
        var response = get(token(data.user(), TRAINER), params("source", source));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).isEmpty();
        assertThat(response.getBody().path("totalElements").asInt()).isZero();
    }

    /**
     * По очереди передаются некорректные статусы, источники, даты, сортировка и параметры страницы. Каждый
     * запрос возвращает ошибку валидации.
     */
    @DisplayName("Недопустимые параметры списка приводят к ошибке 400")
    @ParameterizedTest
    @ValueSource(strings = {"status=INVALID", "source=invalid", "dateFrom=2026-02-30",
        "dateTo=invalid", "dateFrom=2026-11-10&dateTo=2026-11-09", "sort=id,asc",
        "sort=title;drop table workout", "page=-1", "page=2147483648", "page=no",
        "page=2147483647&size=100", "size=0", "size=101"})
    void rejectsInvalidFiltersWithoutExecutingAnUnrestrictedQuery(String query) {
        // Given
        var athlete = account(ATHLETE);
        var values = params();
        for (String pair : query.split("&")) {
            var parts = pair.split("=", 2);
            values.add(parts[0], parts[1]);
        }

        // When
        var response = get(token(athlete, ATHLETE), values);

        // Then
        assertError(
            response,
            400,
            "VALIDATION_ERROR"
        );
    }

    /**
     * Собственная тренировка создана 9 ноября, предоставленная — 10 ноября. Начало периода 10 ноября включает вторую.
     */
    @DisplayName("С нижней границей даты видна только поздняя тренировка")
    @Test
    void filtersFromDate() {
        // Given
        var data = createDatedWorkouts();

        // When
        var response = get(token(data.athlete(), ATHLETE), params("dateFrom", "2026-11-10"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(data.shared().getId().toString());
    }

    /**
     * Конец периода 9 ноября включает собственную тренировку этого дня и исключает предоставленную 10 ноября.
     */
    @DisplayName("С верхней границей даты видна только ранняя тренировка")
    @Test
    void filtersToDate() {
        // Given
        var data = createDatedWorkouts();

        // When
        var response = get(token(data.athlete(), ATHLETE), params("dateTo", "2026-11-09"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(data.own().getId().toString());
    }

    /**
     * Собственная тренировка имеет тип SHARED. При выборе OWN она попадает в ответ, а предоставленная тренером — нет.
     */
    @DisplayName("Фильтр OWN возвращает собственную тренировку атлета")
    @Test
    void filtersOwn() {
        // Given
        var data = createDatedWorkouts();

        // When
        var response = get(token(data.athlete(), ATHLETE), params("source", "OWN"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(data.own().getId().toString());
    }

    /**
     * Атлет выбирает два статуса и два источника, повторяя SHARED. Обе тренировки возвращаются по убыванию даты.
     */
    @DisplayName("При выборе двух источников видны обе тренировки без повторов")
    @Test
    void filtersSourceUnion() {
        // Given
        var data = createDatedWorkouts();

        // When
        var response = get(token(data.athlete(), ATHLETE), params(
            "source",
            "OWN",
            "source",
            "SHARED",
            "source",
            "SHARED",
            "status",
            "CANCELLED",
            "status",
            "COMPLETED"
        ));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).containsExactly(data.shared().getId().toString(), data.own().getId().toString());
    }

    /**
     * Для 25 назначений атлет запрашивает страницу размером 1 или 100. Ответ содержит одну или 25
     * тренировок соответственно и требует не более четырёх SQL-запросов.
     */
    @DisplayName("Страница из одной или 25 тренировок требует не более четырёх запросов к БД")
    @ParameterizedTest
    @CsvSource({"1, 1", "100, 25"})
    void listingDoesNotFetchParticipantsSubscriptionsOrAccountsPerWorkout(int size, int expectedCount) {
        // Given
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        subscribe(
            trainer,
            athlete,
            SubscriptionStatus.ACTIVE
        );
        for (int i = 0; i < 25; i++) {
            participant(
                workout(
                    trainer,
                    ASSIGNED,
                    READY,
                    "План " + i,
                    DAY.plusSeconds(i)
                ),
                trainer,
                athlete
            );
        }
        String token = token(athlete, ATHLETE);
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        long initial = stats.getPrepareStatementCount();

        // When
        var response = get(token, params("size", Integer.toString(size)));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(response.getBody())).hasSize(expectedCount);
        assertThat(stats.getPrepareStatementCount() - initial).isLessThanOrEqualTo(4);
    }

    private record RoleWorkouts(
        UserAccount user,
        Workout own,
        Workout draft,
        Workout other
    ) {}

    private RoleWorkouts createRoleWorkouts() {
        var trainer = account(TRAINER, ATHLETE);
        var otherTrainer = account(TRAINER);
        var own = workout(
            trainer,
            ASSIGNED,
            READY,
            "Сила ног",
            DAY
        );
        var ownDraft = workout(
            trainer,
            SHARED,
            DRAFT,
            "Сила ног черновик",
            DAY
        );
        var ownOther = workout(
            trainer,
            OWN,
            COMPLETED,
            "Другое",
            DAY
        );
        var foreignAssigned = workout(
            otherTrainer,
            ASSIGNED,
            READY,
            "Сила ног извне",
            DAY
        );
        var foreignShared = workout(
            otherTrainer,
            SHARED,
            READY,
            "Сила ног общая",
            DAY
        );
        participant(
            foreignAssigned,
            otherTrainer,
            trainer
        );
        participant(
            foreignShared,
            trainer,
            trainer
        );
        subscribe(
            otherTrainer,
            trainer,
            SubscriptionStatus.ACTIVE
        );
        return new RoleWorkouts(
            trainer,
            own,
            ownDraft,
            ownOther
        );
    }

    private record DatedWorkouts(
        UserAccount athlete,
        Workout own,
        Workout shared
    ) {}

    private DatedWorkouts createDatedWorkouts() {
        var athlete = account(ATHLETE);
        var trainer = account(TRAINER);
        var own = workout(
            athlete,
            SHARED,
            CANCELLED,
            "Своя",
            DAY
        );
        var shared = workout(
            trainer,
            SHARED,
            COMPLETED,
            "Общая",
            DAY.plusSeconds(86400)
        );
        participant(
            shared,
            null,
            athlete
        );
        return new DatedWorkouts(
            athlete,
            own,
            shared
        );
    }

    UserAccount account(UserRole... roles) {
        var result = accounts.saveAndFlush(UserAccount.builder().email(UUID.randomUUID() + "@workout.test")
            .passwordHash("unused").enabled(true).roles(new HashSet<>(List.of(roles))).activeRole(roles[0]).build());
        accountIds.add(result.getId());
        return result;
    }

    Workout workout(
        UserAccount creator,
        WorkoutType type,
        WorkoutStatus status,
        String title,
        Instant date
    ) {
        time.set(date);
        var result = workouts.saveAndFlush(Workout.builder().createdBy(creator.getId()).type(type)
            .status(status).title(title).build());
        workoutIds.add(result.getId());
        return result;
    }

    WorkoutParticipant participant(
        Workout workout,
        UserAccount trainer,
        UserAccount athlete
    ) {
        var result = participants.saveAndFlush(WorkoutParticipant.builder().workout(workout)
            .trainer(trainer).athlete(athlete).build());
        participantIds.add(result.getId());
        return result;
    }

    TrainerSubscription subscribe(
        UserAccount trainer,
        UserAccount athlete,
        SubscriptionStatus status
    ) {
        var result = subscriptions.saveAndFlush(TrainerSubscription.builder().trainer(trainer).athlete(athlete)
            .status(status).build());
        subscriptionIds.add(result.getId());
        return result;
    }

    String token(UserAccount account, UserRole role) { return jwt.issue(account, role).value(); }

    ResponseEntity<JsonNode> get(String token, MultiValueMap<String, String> params) {
        var uri = UriComponentsBuilder.fromUriString("http://localhost:" + port + "/api/v1/workouts")
            .queryParams(params).build().encode().toUri();
        var headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(
            uri,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            JsonNode.class
        );
    }

    MultiValueMap<String, String> params(String... entries) {
        var params = new LinkedMultiValueMap<String, String>();
        for (int i = 0; i < entries.length; i += 2) params.add(entries[i], entries[i + 1]);
        return params;
    }

    List<String> ids(JsonNode body) {
        List<String> ids = new ArrayList<>();
        body.path("items").forEach(item -> ids.add(item.path("id").asText()));
        return ids;
    }

    void assertError(
        ResponseEntity<JsonNode> response,
        int status,
        String code
    ) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
        assertThat(response.getBody().path("status").asInt()).isEqualTo(status);
    }

    @AfterEach
    void cleanup() {
        subscriptions.deleteAllByIdInBatch(subscriptionIds);
        participants.deleteAllByIdInBatch(participantIds);
        workouts.deleteAllByIdInBatch(workoutIds);
        accountIds.forEach(accounts::deleteById);
    }
}
