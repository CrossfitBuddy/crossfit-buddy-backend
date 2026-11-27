package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.nsu.crossfitbuddy.core.controller.Paths;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.JwtService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractWorkoutCreationIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static final String ENDPOINT = "/api" + Paths.WORKOUTS;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate database;
    @Autowired UserAccountRepository accounts;
    @Autowired JwtService jwt;

    protected UUID owner;
    protected UUID exercise;
    protected String token;

    @BeforeEach
    void setUp() {
        owner = account(UserRole.ATHLETE);
        token = token(owner, UserRole.ATHLETE);
        database.update(
            """
            insert into workout_stage(id, code, name, position)
            values (1001, 'TEST_WARMUP', 'Test warmup', 0), (1002, 'TEST_WOD', 'Test WOD', 1)
            on conflict (id) do nothing
            """);
        exercise = database.queryForObject(
            """
            insert into exercise(name, description, created_by)
            values ('Own squat', '', ?)
            returning id
            """,
            UUID.class, owner);
    }

    protected void assertCommittedResponse(JsonNode body) throws Exception {
        var expected = json.treeToValue(body, WorkoutResponse.class);
        String query = new ClassPathResource("sql/read-workout-detail.sql")
            .getContentAsString(StandardCharsets.UTF_8);
        String stored = database.queryForObject(query, String.class, expected.id());
        var actual = json.readValue(stored, WorkoutResponse.class);
        assertThat(actual)
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime.class)
            .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .isEqualTo(expected);
        var listRow = database.queryForMap(
            """
            select id, title, scheduled_at, duration_seconds, type::text as type, status::text as status
            from workout
            where id = ? and created_by = ?
            """,
            expected.id(), owner);
        assertThat(listRow.get("id")).isEqualTo(expected.id());
        assertThat(listRow.get("title")).isEqualTo(expected.name());
        assertThat((Integer) listRow.get("duration_seconds"))
            .isEqualTo(expected.durationSeconds());
        assertThat(listRow.get("type")).isEqualTo("OWN");
        assertThat(listRow.get("status")).isEqualTo("READY");
        assertThat(database.queryForList(
            """
            select f.type
            from complex s
            join complex_format f on f.type = s.complex_format_type
            where s.workout_id = ?
            order by s.position
            """,
            String.class, expected.id()))
            .containsExactlyElementsOf(expected.stages().stream().map(stage -> stage.format().name()).toList());
    }

    protected void assertInvalid(ObjectNode request, String field) throws Exception {
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("fieldErrors[*].field", hasItem(field)));
        assertThat(counts()).isEqualTo(before);
    }

    protected ResultActions create(ObjectNode request, String accessToken) throws Exception {
        return mvc.perform(post(ENDPOINT)
            .contextPath("/api")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(request)));
    }

    protected List<Long> counts() {
        return List.of("workout", "complex", "complex_round", "round_exercise", "workout_participant", "complex_result")
            .stream()
            .map(table -> database.queryForObject("select count(*) from " + table, Long.class))
            .toList();
    }

    protected ObjectNode request(ComplexFormatType format) {
        var stage = json.createObjectNode().put("workoutStageId", 1001).put("format", format.name());
        if (format == ComplexFormatType.AMRAP || format == ComplexFormatType.EMOM) {
            stage.put("timeLimitSeconds", 1200);
        }
        if (format == ComplexFormatType.EMOM) {
            stage.put("intervalSeconds", 60);
        }
        if (format == ComplexFormatType.FOR_TIME) {
            stage.put("targetRounds", 3);
        }
        var round = json.createObjectNode().put("repeatCount", 1);
        var occurrence = json.createObjectNode()
            .put("exerciseId", exercise.toString())
            .put("repetitions", 10);
        round.putArray("exercises").add(occurrence);
        stage.putArray("rounds").add(round);
        var request = json.createObjectNode().put("datetime", "2026-11-16T18:30:00+07:00");
        request.putArray("stages").add(stage);
        return request;
    }

    protected UUID account(UserRole... roles) {
        UUID id = UUID.randomUUID();
        database.update(
            """
            insert into user_account(id, email, password_hash, enabled)
            values (?, ?, 'test-only', true)
            """,
            id, id + "@example.com");
        for (UserRole role : roles) {
            database.update(
                """
                insert into user_roles(user_id, role)
                values (?, cast(? as user_role))
                """,
                id, role.name());
        }
        return id;
    }

    protected String token(UUID id, UserRole role) {
        return jwt.issue(accounts.findById(id).orElseThrow(), role).value();
    }
}
