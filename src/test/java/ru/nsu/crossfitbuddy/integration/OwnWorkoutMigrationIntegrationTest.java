package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.dto.workout.OwnWorkoutRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutRoundRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutStageRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;

import java.util.UUID;

class OwnWorkoutMigrationIntegrationTest extends AbstractRepositoryTest {
    @Test
    @DisplayName("Сохранность старых данных")
    void upgradesExistingWorkoutsWithoutInventingScheduleOrLosingOccurrences() {
        String schema = "workout_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration");
        configuration.target("22").load().migrate();

        var database = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl()
            + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?")
            + "currentSchema="
            + schema, POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID owner = database.queryForObject(
            """
            insert into user_account(email, password_hash)
            values ('legacy@example.com', 'test-only')
            returning id
            """,
            UUID.class);
        database.update(
            """
            insert into stage_format(id, code, name, position)
            values (1, 'WARMUP', 'Warmup', 0)
            """);
        database.update(
            """
            insert into complex_format(id, code, name, description)
            values (1, 'AMRAP', 'Custom AMRAP', 'Existing description')
            """);
        UUID workout = database.queryForObject(
            """
            insert into workout(title, type, created_by)
            values ('Legacy', 'OWN', ?)
            returning id
            """,
            UUID.class, owner);
        UUID first = UUID.fromString("00000000-0000-4000-8000-000000000001");
        UUID second = UUID.fromString("00000000-0000-4000-8000-000000000002");
        for (UUID id : new UUID[] {second, first}) {
            database.update(
                """
                insert into stage(id, workout_id, format_id, complex_format_id,
                created_at, time_limit_seconds, interval_seconds)
                values (?, ?, 1, 1, '2026-01-01T00:00:00Z', 90, 30)
                """,
                id, workout);
        }
        UUID round = database.queryForObject(
            """
            insert into stage_round(stage_id, position)
            values (?, 1)
            returning id
            """,
            UUID.class, first);
        UUID exercise = database.queryForObject(
            """
            insert into exercise(name)
            values ('Squat')
            returning id
            """, UUID.class);
        database.update(
            """
            insert into round_exercise(round_id, exercise_id, position, repetitions)
            values (?, ?, 1, 10), (?, ?, 2, 20)
            """,
            round, exercise, round, exercise);

        configuration.target("latest").load().migrate();

        assertThat(database.queryForList(
            """
            select type
            from workout_format
            order by type
            """, String.class))
            .containsExactly("AMRAP", "EMOM", "FOR_TIME", "TABATA");
        assertThat(database.queryForObject(
            """
            select description
            from legacy_complex_format
            where id = 1
            """,
            String.class))
            .isEqualTo("Existing description");
        assertThat(database.queryForObject(
            """
            select name
            from legacy_complex_format
            where id = 1
            """,
            String.class))
            .isEqualTo("Custom AMRAP");

        assertThat(database.queryForList(
            """
            select workout_format_type
            from stage
            where workout_id = ?
            """,
            String.class, workout))
            .containsOnly("AMRAP");
        assertThat(database.queryForList(
            """
            select params::text
            from stage
            where workout_id = ?
            """,
            String.class, workout))
            .allSatisfy(params ->
            assertThat(params)
            .contains("\"duration\": 1.500", "\"interval\": 0.500"));

        assertThat(database.queryForObject(
            """
            select title
            from workout
            where id = ?
            """, String.class, workout))
            .isEqualTo("Legacy");
        assertThat(database.queryForObject(
            """
            select scheduled_at
            from workout
            where id = ?
            """,
            Object.class, workout))
            .isNull();
        assertThat(database.queryForObject(
            """
            select duration_seconds
            from workout
            where id = ?
            """,
            Integer.class, workout))
            .isNull();
        assertThat(database.queryForList(
            """
            select id
            from stage
            where workout_id = ?
            order by position
            """,
            UUID.class, workout))
            .containsExactly(first, second);
        assertThat(database.queryForList(
            """
            select repetitions
            from round_exercise
            where round_id = ?
            order by position
            """,
            Integer.class, round))
            .containsExactly(10, 20);

        assertThatThrownBy(() ->
            database.update(
            """
            update workout
            set duration_seconds = 0
            where id = ?
            """,
            workout))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
            () -> database.update("""
            update stage
            set position = 0
            where id = ?
            """, first))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
            () -> database.update("""
            update stage
            set position = 1
            where id = ?
            """, second))
            .isInstanceOf(DataIntegrityViolationException.class);
        database.update(
            """
            update workout
            set scheduled_at = '2026-11-16T18:30:00+07:00', duration_seconds = 90
            where id = ?
            """,
            workout);
        assertThat(database.queryForObject(
            """
            select duration_seconds
            from workout
            where id = ?
            """,
            Integer.class, workout))
            .isEqualTo(90);
    }

    @Test
    @DisplayName("Сохранность справочника")
    void preservesDefinitionsAlreadyProvidedByTheFormatModule() {
        String schema = "format_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration");
        configuration.target("24").load().migrate();
        var database = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl()
            + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?")
            + "currentSchema="
            + schema, POSTGRES.getUsername(), POSTGRES.getPassword()));
        database.execute(
            """
            create table workout_format (
            type varchar(32) primary key, name varchar(255) not null,
            description text not null, duration_required boolean not null,
            interval_required boolean not null
            )
            """);
        database.update(
            """
            insert into workout_format
            values ('EMOM', 'Catalog name', 'Catalog description', false, true)
            """);
        configuration.target("latest").load().migrate();
        var definition = database.queryForMap("""
        select *
        from workout_format
        where type = 'EMOM'
        """);
        assertThat(definition)
            .containsEntry("name", "Catalog name")
            .containsEntry("description", "Catalog description")
            .containsEntry("duration_required", false)
            .containsEntry("interval_required", true);
        assertThat(database.queryForObject("""
        select count(*)
        from workout_format
        """, Integer.class))
            .isEqualTo(4);
        assertThat(database.queryForObject(
            """
            select count(*)
            from legacy_complex_format
            """, Integer.class))
            .isEqualTo(4);
        assertThat(database.queryForList(
            """
            select table_name
            from information_schema.tables
            where table_schema = ?
            """,
            String.class, schema))
            .contains("workout_format", "legacy_complex_format")
            .doesNotContain("complex_format");
    }

    @Test
    @DisplayName("Перевод в минуты")
    void convertsStoredSecondsToMinutesWithoutChangingOriginalValues() {
        String schema = "minutes_" + UUID.randomUUID().toString().replace("-", "");
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration");
        configuration.target("25").load().migrate();
        var database = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl()
            + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?")
            + "currentSchema="
            + schema, POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID owner = database.queryForObject(
            """
            insert into user_account(email, password_hash)
            values ('minutes@example.com', 'test')
            returning id
            """,
            UUID.class);
        UUID workout = database.queryForObject(
            """
            insert into workout(type, created_by, duration_seconds)
            values ('OWN', ?, 90)
            returning id
            """,
            UUID.class, owner);
        database.update(
            """
            insert into stage_format(id, code, name, position)
            values (1, 'WOD', 'WOD', 0)
            """);
        UUID stage = database.queryForObject(
            """
            insert into stage(workout_id, format_id, position, workout_format_type, params)
            values (?, 1, 1, 'EMOM', '{"duration":90,"interval":30}')
            returning id
            """,
            UUID.class, workout);
        UUID round = database.queryForObject(
            """
            insert into stage_round(stage_id, position)
            values (?, 1)
            returning id
            """,
            UUID.class, stage);
        UUID exercise = database.queryForObject(
            """
            insert into exercise(name)
            values ('Test')
            returning id
            """, UUID.class);
        database.update(
            """
            insert into round_exercise(round_id, exercise_id, position, duration_seconds)
            values (?, ?, 1, 45)
            """,
            round, exercise);
        configuration.target("latest").load().migrate();
        assertThat(database.queryForObject(
            """
            select duration_minutes
            from workout
            where id = ?
            """,
            java.math.BigDecimal.class, workout))
            .isEqualByComparingTo("1.5");
        assertThat(database.queryForObject(
            """
            select (params->>'duration')::numeric
            from stage
            where id = ?
            """,
            java.math.BigDecimal.class, stage))
            .isEqualByComparingTo("1.5");
        assertThat(database.queryForObject(
            """
            select (params->>'interval')::numeric
            from stage
            where id = ?
            """,
            java.math.BigDecimal.class, stage))
            .isEqualByComparingTo("0.5");
        assertThat(database.queryForObject(
            """
            select duration_minutes
            from round_exercise
            where round_id = ?
            """,
            java.math.BigDecimal.class, round))
            .isEqualByComparingTo("0.75");
        assertThat(database.queryForObject(
            """
            select duration_seconds
            from workout
            where id = ?
            """,
            Integer.class, workout))
            .isEqualTo(90);
        assertThat(database.queryForObject(
            """
            select duration_seconds
            from round_exercise
            where round_id = ?
            """,
            Integer.class, round))
            .isEqualTo(45);
    }

    @Test
    @DisplayName("Старые FOR_TIME")
    void normalizesLegacyForTimeWithoutLosingItsParameters() {
        String schema = "for_time_" + UUID.randomUUID().toString().replace("-", "");
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema).defaultSchema(schema).locations("classpath:db/migration");
        configuration.target("25").load().migrate();
        var database = new JdbcTemplate(new DriverManagerDataSource(
            POSTGRES.getJdbcUrl() + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema,
            POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID owner = database.queryForObject(
            "insert into user_account(email, password_hash) values ('legacy-ft@example.com', 'test') returning id",
            UUID.class);
        UUID workout = database.queryForObject(
            "insert into workout(type, created_by, duration_seconds) values ('OWN', ?, 1) returning id",
            UUID.class, owner);
        database.update("insert into stage_format(id, code, name, position) values (1, 'WOD', 'WOD', 0)");
        UUID stage = database.queryForObject("""
            insert into stage(workout_id, format_id, position, workout_format_type, params)
            values (?, 1, 1, 'FOR_TIME', '{"duration":90}') returning id
            """, UUID.class, workout);
        configuration.target("latest").load().migrate();
        assertThat(database.queryForObject("select params::text from stage where id = ?", String.class, stage))
            .isEqualTo("{}");
        assertThat(database.queryForObject(
            "select (legacy_params->>'duration')::numeric from stage where id = ?", java.math.BigDecimal.class, stage))
            .isEqualByComparingTo("1.5");
        var minutes = database.queryForObject(
            "select duration_minutes from workout where id = ?", java.math.BigDecimal.class, workout);
        var exercise = new WorkoutExerciseRequest(UUID.randomUUID(), 1, null, null, null, null, null);
        var round = new WorkoutRoundRequest(1, java.util.List.of(exercise));
        var stageRequest = new WorkoutStageRequest(1, WorkoutFormatType.FOR_TIME, java.util.Map.of(), java.util.List.of(round));
        var request = new OwnWorkoutRequest(null, null, java.time.OffsetDateTime.now(), minutes, java.util.List.of(stageRequest));
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
    }

}
