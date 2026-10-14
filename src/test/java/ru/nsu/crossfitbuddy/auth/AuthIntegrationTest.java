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

import ru.nsu.crossfitbuddy.auth.security.JwtProperties;
import ru.nsu.crossfitbuddy.auth.security.JwtService;
import ru.nsu.crossfitbuddy.core.user.domain.UserRole;
import ru.nsu.crossfitbuddy.core.user.persistence.AthleteProfileRepository;
import ru.nsu.crossfitbuddy.core.user.persistence.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.user.persistence.UserProfileRepository;
import ru.nsu.crossfitbuddy.support.PostgresContainerSupport;

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
class AuthIntegrationTest extends PostgresContainerSupport {
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
        mvc.perform(get("/api/v1/trainer/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
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
    void switchesAssignedRoleRemembersItAndRejectsPrivilegeEscalation() throws Exception {
        String email = generateUniqueEmail();
        UUID id = createTestAccount(email, true, UserRole.ATHLETE, UserRole.TRAINER);
        String originalToken = extractAccessToken(login(email, PASSWORD).andExpect(status().isOk()));
        String trainerToken = extractAccessToken(mvc
            .perform(patch("/api/v1/auth/active-role")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + originalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"TRAINER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("user.activeRole").value("TRAINER")));
        mvc.perform(get("/api/v1/trainer/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + trainerToken))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/athlete/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + trainerToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + originalToken))
            .andExpect(jsonPath("activeRole").value("ATHLETE"));
        assertThat(userAccountRepository.findById(id).orElseThrow().getActiveRole()).isEqualTo(UserRole.TRAINER);
        login(email, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("user.activeRole").value("TRAINER"));
        mvc.perform(patch("/api/v1/auth/active-role")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
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
        mvc.perform(get("/api/v1/probe-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
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
            .isEqualTo(3);
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
    void migratesExistingV1AccountWithoutLosingCredentialsRolesOrReferences() {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration");
        configuration.target("1").load().migrate();
        UUID userId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();
        String originalHash = passwordEncoder.encode(PASSWORD);
        database.update("insert into " + schema + ".app_user(id, email, password_hash, display_name) values (?,"
                + " 'OLD@EXAMPLE.COM', ?, 'Legacy')", userId, originalHash);
        database.update("insert into " + schema + ".user_roles(user_id, role) values (?, 'ATHLETE')", userId);
        database.update("insert into " + schema + ".workout(id, title, type, created_by) values (?, 'Legacy workout',"
                + " 'OWN', ?)", workoutId, userId);
        configuration.target("latest").load().migrate();
        assertThat(database
            .queryForObject("select email from " + schema + ".user_account where id = ?", String.class, userId))
            .isEqualTo("old@example.com");
        assertThat(database
            .queryForObject("select password_hash from " + schema + ".user_account where id = ?", String.class, userId))
            .isEqualTo(originalHash);
        assertThat(database
            .queryForObject("select role::text from " + schema + ".user_roles where user_id = ?", String.class, userId))
            .isEqualTo("ATHLETE");
        assertThat(database
            .queryForObject("select created_by from " + schema + ".workout where id = ?", UUID.class, workoutId))
            .isEqualTo(userId);
        assertThat(database
            .queryForObject("select display_name from " + schema + ".user_profile where user_id = ?", String.class,
                userId)).isEqualTo("Legacy");
        assertThat(database
            .queryForObject("select count(*) from " + schema + ".athlete_profile where user_id = ?", Integer.class,
                userId)).isEqualTo(1);
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
    void migratesV2NamesAccountStateAndBothRoleProfilesKeepingAllIncomingForeignKeys() {
        String schema = "profiles_" + UUID.randomUUID().toString().replace("-", "");
        var configuration = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration");
        configuration.target("2").load().migrate();
        UUID dualRoleAccountId = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        String hash = passwordEncoder.encode(PASSWORD);
        database
            .update("insert into " + schema + ".app_user(id, email, password_hash, display_name, first_name, last_name,"
                + " active_role, enabled, deleted, token_version, version) values (?,"
                + " 'dual@example.com', ?, 'Иван Иванов', 'Иван', 'Иванов', 'TRAINER'," + " false, true, 12, 7)",
                dualRoleAccountId, hash);
        database.update("insert into " + schema + ".app_user(id, email, password_hash, display_name) values (?,"
                + " 'trainer@example.com', ?, 'Legacy trainer')", trainerId, hash);
        database.update("insert into " + schema + ".app_user(id, email, password_hash, display_name) values (?,"
                + " 'admin@example.com', ?, 'Admin')", adminId, hash);
        database
            .update("insert into " + schema + ".user_roles(user_id, role) values (?, 'ATHLETE'), (?, 'TRAINER'), (?,"
                + " 'TRAINER'), (?, 'ADMIN')", dualRoleAccountId, dualRoleAccountId, trainerId, adminId);
        database.update("insert into " + schema + ".workout(id, title, type, created_by) values (?,"
                + " 'Assigned', 'ASSIGNED', ?)", workoutId, trainerId);
        database.update("insert into " + schema + ".workout_participant(workout_id, trainer_id, athlete_id)"
                + " values (?, ?, ?)", workoutId, trainerId, dualRoleAccountId);
        database.update("insert into " + schema + ".trainer_subscription(athlete_id, trainer_id, status) values (?, ?,"
                + " 'ACTIVE')", dualRoleAccountId, trainerId);
        database.update("insert into " + schema + ".stage_format(id, code, name, position)"
                + " values (1, 'TEST', 'Test stage format', 0)");
        database.update("insert into " + schema + ".complex_format(id, code, name)"
                + " values (1, 'TEST', 'Test complex format')");
        database.update("insert into " + schema + ".stage(id, workout_id, format_id, complex_format_id)"
                + " values (?, ?, 1, 1)", stageId, workoutId);
        database.update("insert into " + schema + ".stage_result(stage_id, athlete_id) values (?, ?)",
            stageId, dualRoleAccountId);
        database
            .update("insert into " + schema + ".exercise(name, created_by) values ('Exercise', ?)", dualRoleAccountId);
        database.update("insert into " + schema + ".password_reset_code(user_id, code_hash, expires_at) values (?,"
                + " 'existing-code-hash', now() + interval '15 minutes')", dualRoleAccountId);
        configuration.target("latest").load().migrate();

        var migrated = database
            .queryForMap("select * from " + schema + ".user_account where id = ?", dualRoleAccountId);
        assertThat(migrated.get("id")).isEqualTo(dualRoleAccountId);
        assertThat(migrated.get("password_hash")).isEqualTo(hash);
        assertThat(migrated.get("active_role").toString()).isEqualTo("TRAINER");
        assertThat(migrated.get("enabled")).isEqualTo(false);
        assertThat(migrated.get("deleted")).isEqualTo(true);
        assertThat(migrated.get("token_version")).isEqualTo(12L);
        assertThat(migrated.get("version")).isEqualTo(7L);
        assertThat(migrated).doesNotContainKeys("first_name", "last_name", "display_name");
        var profile = database
            .queryForMap("select * from " + schema + ".user_profile where user_id = ?", dualRoleAccountId);
        assertThat(profile.get("first_name")).isEqualTo("Иван");
        assertThat(profile.get("last_name")).isEqualTo("Иванов");
        assertThat(profile.get("display_name")).isEqualTo("Иван Иванов");
        assertThat(profile.get("birth_date")).isNull();
        assertThat(profile.get("avatar_key")).isNull();
        assertThat(database.queryForObject("select count(*) from " + schema + ".user_profile", Integer.class))
            .isEqualTo(3);
        assertThat(database.queryForObject("select count(*) from " + schema + ".athlete_profile", Integer.class))
            .isEqualTo(1);
        assertThat(database.queryForObject("select count(*) from " + schema + ".trainer_profile", Integer.class))
            .isEqualTo(2);
        assertThat(database
            .queryForObject("select weight_kg from " + schema + ".athlete_profile where user_id = ?",
                java.math.BigDecimal.class, dualRoleAccountId)).isNull();
        assertThat(database
            .queryForObject("select athlete_id from " + schema + ".workout_participant where workout_id = ?",
                UUID.class, workoutId))
            .isEqualTo(dualRoleAccountId);
        assertThat(database
            .queryForObject("select trainer_id from " + schema + ".workout_participant where workout_id = ?",
                UUID.class, workoutId)).isEqualTo(trainerId);
        assertThat(database
            .queryForObject("select athlete_id from " + schema + ".stage_result where stage_id = ?",
                UUID.class, stageId)).isEqualTo(dualRoleAccountId);
        assertThat(database
            .queryForObject("select code_hash from " + schema + ".password_reset_code where user_id = ?", String.class,
                dualRoleAccountId)).isEqualTo("existing-code-hash");
        assertThat(database
            .queryForList("select c.conrelid::regclass::text || '.' || a.attname from"
                + " pg_constraint c join pg_attribute a on a.attrelid ="
                + " c.conrelid and a.attnum = any(c.conkey) where c.confrelid ="
                + " cast(? as regclass) and c.contype = 'f'", String.class, schema + ".user_account"))
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
        assertThat(database
            .queryForObject("select count(*) from information_schema.tables where table_schema"
                + " = ? and table_name = 'profile_history'", Integer.class, schema)).isZero();
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
        @GetMapping({ "/api/v1/athlete/probe", "/api/v1/trainer/probe", "/api/v1/admin/probe" })
        String probe() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/v1/probe-admin")
        String methodProtected() {
            return "ok";
        }
    }
}
