package ru.nsu.crossfitbuddy.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class FlywaySchemaIntegrationTest extends AbstractRepositoryTest {

    @Autowired
    JdbcTemplate database;

    @Test
    void appliesSeedRolesAndPasswordRecoveryConstraints() {
        assertThat(database.queryForObject("select count(*) from roles", Integer.class))
            .isEqualTo(3);

        var definitions = database.queryForList("""
            select pg_get_constraintdef(oid)
            from pg_constraint
            where conrelid = 'public.password_reset_code'::regclass
            """, String.class);

        assertThat(definitions)
            .anyMatch(definition -> definition.startsWith("UNIQUE (user_id)"));

        assertThat(definitions)
            .anyMatch(definition -> definition.contains("failed_attempts >= 0"));

        assertThat(definitions)
            .anyMatch(definition -> definition.contains("expires_at > created_at"));
    }

    @Test
    void createsFinalSchemaFromScratch() {
        String schema = "fresh_" + UUID.randomUUID().toString().replace("-", "");

        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration")
            .load()
            .migrate();

        assertThat(database.queryForObject(
            "select count(*) from " + schema + ".roles",
            Integer.class
        )).isEqualTo(3);

        assertThat(database.queryForList("""
            select column_name
            from information_schema.columns
            where table_schema = ? and table_name = 'user_account'
            """, String.class, schema))
            .contains("email", "password_hash", "deleted", "token_version", "version")
            .doesNotContain("first_name", "last_name", "display_name");

        assertThat(database.queryForList("""
            select table_name
            from information_schema.tables
            where table_schema = ?
            """, String.class, schema))
            .contains("user_account", "user_profile", "athlete_profile", "trainer_profile")
            .doesNotContain("app_user", "profile_history");
    }
}
