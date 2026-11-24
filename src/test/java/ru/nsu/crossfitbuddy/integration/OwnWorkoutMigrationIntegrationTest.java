package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@Testcontainers
class OwnWorkoutMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Test
    @DisplayName("Обновление общей схемы")
    void preservesExistingComplexesAndSecondsWhenExtendingWorkout() {
        String schema = "workout_" + UUID.randomUUID().toString().replace("-", "");
        var flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema).defaultSchema(schema).target("24").load();
        flyway.migrate();
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl() + "&currentSchema=" + schema,
            POSTGRES.getUsername(), POSTGRES.getPassword());
        var database = new JdbcTemplate(source);
        UUID owner = database.queryForObject("""
            insert into user_account(email, password_hash) values ('migration@example.com', 'test-only') returning id
            """, UUID.class);
        UUID workout = database.queryForObject("""
            insert into workout(title, type, created_by) values ('Existing', 'OWN', ?) returning id
            """, UUID.class, owner);
        UUID complex = database.queryForObject("""
            insert into complex(workout_id, workout_stage_id, complex_format_type, time_limit_seconds, interval_seconds)
            values (?, 1, 'EMOM', 1200, 60) returning id
            """, UUID.class, workout);
        UUID second = database.queryForObject("""
            insert into complex(workout_id, workout_stage_id, complex_format_type, target_rounds, created_at)
            values (?, 2, 'FOR_TIME', 5, now() + interval '1 second') returning id
            """, UUID.class, workout);
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema).defaultSchema(schema).load().migrate();
        assertThat(database.queryForList("select id from complex where workout_id = ? order by position",
            UUID.class, workout)).containsExactly(complex, second);
        var row = database.queryForMap("select * from complex where id = ?", complex);
        assertThat(row.get("time_limit_seconds")).isEqualTo(1200);
        assertThat(row.get("interval_seconds")).isEqualTo(60);
        assertThat(row).doesNotContainKeys("params", "legacy_params", "workout_format_type");
        assertThat(database.queryForObject("select target_rounds from complex where id = ?",
            Integer.class, second)).isEqualTo(5);
        var header = database.queryForMap("select * from workout where id = ?", workout);
        assertThat(header.get("title")).isEqualTo("Existing");
        assertThat(header.get("scheduled_at")).isNull();
        assertThat(header.get("coach")).isNull();
        assertThat(database.queryForList("select type from complex_format order by type", String.class))
            .containsExactly("AMRAP", "EMOM", "FOR_TIME", "FREE");
        assertThat(database.queryForObject("""
            select count(*) from information_schema.columns
            where table_schema = ? and table_name = 'complex_format'
              and column_name in ('duration_required', 'interval_required')
            """, Integer.class, schema)).isZero();
        assertThat(database.queryForObject("""
            select count(*) from information_schema.tables
            where table_schema = ? and table_name in ('workout_format', 'legacy_complex_format')
            """, Integer.class, schema)).isZero();
    }
}
