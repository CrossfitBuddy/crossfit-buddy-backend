package ru.nsu.crossfitbuddy.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Testcontainers;

import ru.nsu.crossfitbuddy.auth.security.annotation.AdminAuthority;
import ru.nsu.crossfitbuddy.auth.security.annotation.AthleteAuthority;
import ru.nsu.crossfitbuddy.auth.security.annotation.TrainerAuthority;
import ru.nsu.crossfitbuddy.core.entity.JwtProperties;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import ru.nsu.crossfitbuddy.core.repository.AthleteProfileRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.repository.UserProfileRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Import(AuthIntegrationTest.RoleEndpoints.class)
@Testcontainers
class AuthIntegrationTest extends AbstractRepositoryTest {
    private static final String PASSWORD = "StrongPassword12345";
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate database;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    UserProfileRepository userProfileRepository;
    @Autowired
    AthleteProfileRepository athleteProfileRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;

    @Test
    void registersHashesPasswordAndAuthenticatesUsingReturnedToken() throws Exception {
        String email = generateUniqueEmail();
        var result = register(email, PASSWORD, PASSWORD)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("user.roles[0]").value("ATHLETE"))
            .andExpect(jsonPath("user.activeRole").value("ATHLETE"))
            .andExpect(jsonPath("user.passwordHash").doesNotExist())
            .andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        var account = userAccountRepository.findByEmail(email).orElseThrow();
        assertThat(account.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, account.getPasswordHash())).isTrue();
        assertThat(userProfileRepository.findById(account.getId()).orElseThrow().getFirstName()).isEqualTo("Иван");
        assertThat(athleteProfileRepository.findById(account.getId())).isPresent();
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("email").value(email));
        mvc.perform(get("/api/v1/workouts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void loginNormalizesEmailAndReturnsOnlyActiveRoleAuthority() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE, UserRole.TRAINER);
        String token = extractAccessToken(login(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", PASSWORD)
            .andExpect(status().isOk()));
        mvc.perform(get("/api/v1/athlete/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/trainer/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("code").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsWrongPasswordAndUnknownEmail() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);
        login(email, "wrong-password")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("code").value("INVALID_CREDENTIALS"));
        login(generateUniqueEmail(), PASSWORD)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void returnsConsistentErrorMessagesWithoutExposingCredentials() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);
        login(email, "incorrect-password")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("timestamp").isNotEmpty())
            .andExpect(jsonPath("status").value(401))
            .andExpect(jsonPath("code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("message").value("Invalid email or password."))
            .andExpect(jsonPath("path").value("/api/v1/auth/login"))
            .andExpect(jsonPath("password").doesNotExist());
        register(generateUniqueEmail(), PASSWORD, "DifferentPassword123")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Passwords do not match."));
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("message").value("Authentication is required or the access token is" + " invalid."));
        String token = jwtService.issue(userAccountRepository.findByEmail(email).orElseThrow(), UserRole.ATHLETE)
            .value();
        mvc.perform(get("/api/v1/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("message").value("You do not have permission to perform this action."));
    }

    @Test
    void returnsExplicitAndDeterministicValidationMessagesRegardlessOfRequestLocale() throws Exception {
        for (String locale : new String[] { "ru", "en" }) {
            mvc.perform(post("/api/v1/auth/login")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("email", "invalid-email", "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("message").value("email: Enter a valid email address."));
        }
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", generateUniqueEmail()))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("password: Password is required."));
        var body = registrationBody(generateUniqueEmail(), "", "");
        body.put("firstName", "");
        body.put("lastName", "");
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("firstName: First name is required."));
    }

    @Test
    void blocksDisabledAndDeletedAccounts() throws Exception {
        String email = generateUniqueEmail();
        UUID id = createTestAccount(email, false, UserRole.ATHLETE);
        login(email, "wrong-password")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("code").value("INVALID_CREDENTIALS"));
        login(email, PASSWORD)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("code").value("ACCOUNT_DISABLED"));
        database.update("update user_account set enabled = true, deleted = true where id = ?", id);
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAccountsWithoutRolesAndPrioritizesAdmin() throws Exception {
        String accountWithoutRolesEmail = generateUniqueEmail();
        createTestAccount(accountWithoutRolesEmail, true);
        login(accountWithoutRolesEmail, PASSWORD)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("code").value("ROLE_NOT_ASSIGNED"));
        String adminEmail = generateUniqueEmail();
        UUID id = createTestAccount(adminEmail, true, UserRole.ADMIN, UserRole.ATHLETE);
        database.update("update user_account set active_role = 'ATHLETE' where id = ?", id);
        String token = extractAccessToken(login(adminEmail, PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("user.activeRole").value("ADMIN")));
        mvc.perform(get("/api/v1/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/auth/active-role")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ATHLETE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void returns401WithoutTokenAndDoesNotLeakSecurityContextAcrossRequests() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);
        String token = extractAccessToken(login(email, PASSWORD).andExpect(status().isOk()));
        mvc.perform(get("/api/v1/workouts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/workouts"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer one", "Bearer two"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedExpiredAndForeignSignedTokens() throws Exception {
        UUID id = createTestAccount(generateUniqueEmail(), true, UserRole.ATHLETE);
        var account = userAccountRepository.findById(id).orElseThrow();
        String expired = new JwtService(new JwtProperties(SECRET, "crossfitbuddy", Duration.ofMinutes(15)), Clock
            .fixed(Instant.now().minusSeconds(1000), ZoneOffset.UTC)).issue(account, UserRole.ATHLETE).value();
        String foreign = new JwtService(new JwtProperties("YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=",
            "crossfitbuddy", Duration.ofMinutes(15)), Clock.systemUTC()).issue(account, UserRole.ATHLETE).value();
        for (String token : new String[] { "garbage", "", expired, foreign }) {
            mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void checksAccountStateAndRoleMembershipOnEveryRequest() throws Exception {
        UUID id = createTestAccount(generateUniqueEmail(), true, UserRole.ATHLETE);
        String token = jwtService.issue(userAccountRepository.findById(id).orElseThrow(), UserRole.ATHLETE).value();
        database.update("update user_account set enabled = false where id = ?", id);
        protectedRequest(token).andExpect(status().isUnauthorized());
        database.update("update user_account set enabled = true, deleted = true where id = ?", id);
        protectedRequest(token).andExpect(status().isUnauthorized());
        database.update("update user_account set deleted = false, token_version = 1 where id = ?", id);
        protectedRequest(token).andExpect(status().isUnauthorized());
        database.update("update user_account set token_version = 0 where id = ?", id);
        database.update("delete from user_roles where user_id = ?", id);
        protectedRequest(token).andExpect(status().isUnauthorized());
        database.update("delete from user_account where id = ?", id);
        protectedRequest(token).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesRegistrationAndRejectsClientSuppliedRoles() throws Exception {
        register(generateUniqueEmail(), "short1", "short1").andExpect(status().isBadRequest());
        register(generateUniqueEmail(), "OnlyLettersPassword", "OnlyLettersPassword")
            .andExpect(status().isBadRequest());
        register(generateUniqueEmail(), PASSWORD, "DifferentPassword123")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("PASSWORD_MISMATCH"));
        register("not-email", PASSWORD, PASSWORD).andExpect(status().isBadRequest());
        String longUtf8 = "Я".repeat(40) + "123";
        register(generateUniqueEmail(), longUtf8, longUtf8)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("PASSWORD_TOO_LONG"));
        var body = registrationBody(generateUniqueEmail(), PASSWORD, PASSWORD);
        body.put("roles", new String[] { "ADMIN" });
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAmbiguousJsonAndOversizedFieldValues() throws Exception {
        for (String body : new String[] { "{\"email\":\"one@example.com\",\"email\":\"two@example.com\",\"password\":\""
            + PASSWORD + "\"}", "{\"email\":\"one@example.com\",\"password\":\"" + PASSWORD + "\"} {}",
            "{\"email\":\"one@example.com\",\"password\":\"" + "a".repeat(9000) + "\"}" }) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void securityFailuresRetainProtectionHeadersAndPublicLoginIgnoresStaleToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")));
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);
        mvc.perform(post("/api/v1/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isOk());
    }

    @Test
    void rejectsUntrustedCorsOriginsAndDoesNotAllowCookieCredentials() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void rejectsCaseInsensitiveDuplicateEmailIncludingConcurrentRegistrations() throws Exception {
        String email = generateUniqueEmail();
        register(email, PASSWORD, PASSWORD).andExpect(status().isCreated());
        register(email.toUpperCase(java.util.Locale.ROOT), PASSWORD, PASSWORD).andExpect(status().isConflict());
        String concurrentEmail = generateUniqueEmail();
        var one = CompletableFuture.supplyAsync(() -> registrationStatus(concurrentEmail));
        var two = CompletableFuture.supplyAsync(() -> registrationStatus(concurrentEmail));
        assertThat(Arrays.asList(one.join(), two.join())).containsExactlyInAnyOrder(201, 409);
        assertThat(database
            .queryForObject("select count(*) from user_account where email = ?", Long.class, concurrentEmail))
            .isEqualTo(1);
    }

    @Test
    void checksMethodSecurityAndCorsPreflight() throws Exception {
        UUID id = createTestAccount(generateUniqueEmail(), true, UserRole.ATHLETE);
        String token = jwtService.issue(userAccountRepository.findById(id).orElseThrow(), UserRole.ATHLETE).value();
        mvc.perform(get("/api/v1/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void appliesMigrationsAndEnforcesRoleAndRecoveryConstraints() {
        assertThat(database.queryForObject("select count(*) from roles", Integer.class)).isEqualTo(3);
        assertThat(database.queryForObject("select count(*) from flyway_schema_history where success", Integer.class))
            .isEqualTo(19);
        UUID id = createTestAccount(generateUniqueEmail(), true, UserRole.ATHLETE);
        assertThatThrownBy(() -> database
            .update("insert into password_reset_code(user_id, code_hash,"
                + " expires_at, failed_attempts) values (?, 'hash'," + " now() + interval '15 minutes', 6)", id))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> database
            .update("insert into password_reset_code(user_id, code_hash,"
                + " expires_at) values (?, 'hash', now() + interval '16" + " minutes')", id))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void createsFinalSchemaFromScratch() {
        String schema = "fresh_" + UUID.randomUUID().toString().replace("-", "");
        var result = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration")
            .load().migrate();
        assertThat(result.migrationsExecuted).isEqualTo(19);
        assertThat(database.queryForObject("select count(*) from " + schema + ".roles", Integer.class))
            .isEqualTo(3);
        assertThat(database.queryForList(
            "select column_name from information_schema.columns where table_schema = ? and table_name = 'user_account'",
            String.class, schema)).contains("email", "password_hash", "deleted", "token_version", "version")
            .doesNotContain("first_name", "last_name", "display_name");
        assertThat(database.queryForList(
            "select table_name from information_schema.tables where table_schema = ?", String.class, schema))
            .contains("user_account", "user_profile", "athlete_profile", "trainer_profile")
            .doesNotContain("app_user", "profile_history");
    }

    @Test
    void selectsTrainerOnlyAndFallsBackWhenRememberedRoleWasRevoked() throws Exception {
        String trainerEmail = generateUniqueEmail();
        createTestAccount(trainerEmail, true, UserRole.TRAINER);
        login(trainerEmail, PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("user.activeRole").value("TRAINER"));
        String athleteEmail = generateUniqueEmail();
        UUID id = createTestAccount(athleteEmail, true, UserRole.ATHLETE);
        database.update("update user_account set active_role = 'TRAINER' where id = ?", id);
        login(athleteEmail, PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("user.activeRole").value("ATHLETE"));
    }

    @Test
    void createsAllIncomingAccountForeignKeys() {
        String schema = "public";
        assertThat(database
            .queryForList("select n.nspname || '.' || c.relname || '.' || a.attname from"
                + " pg_constraint fk join pg_class c on c.oid = fk.conrelid"
                + " join pg_namespace n on n.oid = c.relnamespace join pg_attribute a on a.attrelid ="
                + " c.oid and a.attnum = any(fk.conkey) where fk.confrelid ="
                + " cast(? as regclass) and fk.contype = 'f'", String.class, schema + ".user_account"))
            .containsExactlyInAnyOrder(
                schema + ".user_roles.user_id",
                schema + ".exercise.created_by",
                schema + ".workout.created_by",
                schema + ".workout_participant.athlete_id",
                schema + ".workout_participant.trainer_id",
                schema + ".trainer_subscription.athlete_id",
                schema + ".trainer_subscription.trainer_id",
                schema + ".stage_result.athlete_id",
                schema + ".password_reset_code.user_id",
                schema + ".user_profile.user_id",
                schema + ".athlete_profile.user_id",
                schema + ".trainer_profile.user_id");
    }

    private ResultActions protectedRequest(String token) throws Exception {
        return mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private UUID createTestAccount(String email, boolean enabled, UserRole... roles) {
        UUID id = UUID.randomUUID();
        database.update("insert into user_account(id, email, password_hash, enabled) values (?, ?, ?, ?)", id, email,
                passwordEncoder.encode(PASSWORD), enabled);
        database.update("insert into user_profile(user_id, display_name) values (?, 'Test')", id);
        for (UserRole role : roles) {
            database.update("insert into user_roles(user_id, role) values (?, cast(? as user_role))", id, role.name());
        }
        return id;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private ResultActions register(String email, String password, String confirmation) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(registrationBody(email, password, confirmation))));
    }

    private Map<String, Object> registrationBody(String email, String password, String confirmation) {
        return new java.util.HashMap<>(Map.of(
            "firstName", "Иван",
            "lastName", "Иванов",
            "email", email,
            "password", password,
            "passwordConfirmation", confirmation));
    }

    private int registrationStatus(String email) {
        try {
            return register(email, PASSWORD, PASSWORD).andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String extractAccessToken(ResultActions result) throws Exception {
        JsonNode response = json.readTree(result.andReturn().getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private String generateUniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    @RestController
    static class RoleEndpoints {

        @AthleteAuthority
        @GetMapping({ "/api/v1/athlete/probe" })
        String athleteProbe() {
            return "ok";
        }

        @TrainerAuthority
        @GetMapping({ "/api/v1/trainer/probe" })
        String trainerProbe() {
            return "ok";
        }

        @AdminAuthority
        @GetMapping("/api/v1/admin/probe")
        String adminProbe() {
            return "ok";
        }
    }
}
