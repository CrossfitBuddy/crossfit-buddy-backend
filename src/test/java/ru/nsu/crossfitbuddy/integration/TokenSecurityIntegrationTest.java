package ru.nsu.crossfitbuddy.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.crossfitbuddy.auth.security.annotation.AdminAuthority;
import ru.nsu.crossfitbuddy.auth.security.annotation.AthleteAuthority;
import ru.nsu.crossfitbuddy.auth.security.annotation.TrainerAuthority;
import ru.nsu.crossfitbuddy.core.controller.Paths;
import ru.nsu.crossfitbuddy.core.entity.JwtProperties;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.JwtService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Import(TokenSecurityIntegrationTest.RoleEndpoints.class)
class TokenSecurityIntegrationTest extends AbstractRepositoryTest {
    private static final String PASSWORD = "StrongPassword12345";

    private static final String AUTH_LOGIN_PATH = Paths.AUTH + Paths.AUTH_LOGIN;
    private static final String AUTH_ME_PATH = Paths.AUTH + Paths.AUTH_ME;
    private static final String AUTH_ACTIVE_ROLE_PATH = Paths.AUTH + Paths.AUTH_ACTIVE_ROLE;

    private static final String TEST_ATHLETE_ENDPOINT = "/v1/athlete/probe";
    private static final String TEST_TRAINER_ENDPOINT = "/v1/trainer/probe";
    private static final String TEST_ADMIN_ENDPOINT = "/v1/admin/probe";

    @Value("${app.jwt.secret}")
    private String secret;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate database;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    @Test
    void loginNormalizesEmailAndReturnsOnlyActiveRoleAuthority() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE, UserRole.TRAINER);

        String token = extractAccessToken(login(" " + email.toUpperCase(Locale.ROOT) + " ")
            .andExpect(status().isOk()));

        mvc.perform(get(TEST_ATHLETE_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        mvc.perform(get(TEST_TRAINER_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        mvc.perform(get(TEST_ADMIN_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("code").value("ACCESS_DENIED"));
    }

    @Test
    void deniesAdminEndpointForAthleteTokenWithConsistentMessage() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);

        String token = jwtService.issue(
            userAccountRepository.findByEmail(email).orElseThrow(),
            UserRole.ATHLETE
        ).value();

        mvc.perform(get(TEST_ADMIN_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("message").value("You do not have permission to perform this action."));
    }

    @Test
    void deniesAccountsWithoutRolesAndPrioritizesAdmin() throws Exception {
        String accountWithoutRolesEmail = generateUniqueEmail();
        createTestAccount(accountWithoutRolesEmail, true);

        login(accountWithoutRolesEmail)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("code").value("ROLE_NOT_ASSIGNED"));

        String adminEmail = generateUniqueEmail();
        UUID id = createTestAccount(adminEmail, true, UserRole.ADMIN, UserRole.ATHLETE);

        database.update("update user_account set active_role = 'ATHLETE' where id = ?", id);

        String token = extractAccessToken(login(adminEmail)
            .andExpect(status().isOk())
            .andExpect(jsonPath("user.activeRole").value("ADMIN")));

        mvc.perform(get(TEST_ADMIN_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        mvc.perform(patch(AUTH_ACTIVE_ROLE_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ATHLETE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void returns401WithoutTokenAndDoesNotLeakSecurityContextAcrossRequests() throws Exception {
        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);

        String token = extractAccessToken(login(email)
            .andExpect(status().isOk()));

        mvc.perform(get(Paths.WORKOUTS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        mvc.perform(get(Paths.WORKOUTS))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(jsonPath("code").value("UNAUTHORIZED"));

        mvc.perform(get(AUTH_ME_PATH).header(HttpHeaders.AUTHORIZATION, "Basic abc"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get(AUTH_ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer one", "Bearer two"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedExpiredAndForeignSignedTokens() throws Exception {
        UUID id = createTestAccount(generateUniqueEmail(), true, UserRole.ATHLETE);
        var account = userAccountRepository.findById(id).orElseThrow();

        String expired = new JwtService(
            new JwtProperties(secret, "crossfitbuddy", Duration.ofMinutes(15)),
            Clock.fixed(Instant.now().minusSeconds(1000), ZoneOffset.UTC)
        ).issue(account, UserRole.ATHLETE).value();

        String foreign = new JwtService(
            new JwtProperties(
                "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=",
                "crossfitbuddy",
                Duration.ofMinutes(15)
            ),
            Clock.systemUTC()
        ).issue(account, UserRole.ATHLETE).value();

        for (String token : new String[]{"garbage", "", expired, foreign}) {
            mvc.perform(get(AUTH_ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void checksAccountStateAndRoleMembershipOnEveryRequest() throws Exception {
        UUID id = createTestAccount(generateUniqueEmail(), true, UserRole.ATHLETE);
        String token = jwtService.issue(
            userAccountRepository.findById(id).orElseThrow(),
            UserRole.ATHLETE
        ).value();

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
    void securityFailuresRetainProtectionHeadersAndPublicLoginIgnoresStaleToken() throws Exception {
        mvc.perform(get(AUTH_ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));

        String email = generateUniqueEmail();
        createTestAccount(email, true, UserRole.ATHLETE);

        mvc.perform(post(AUTH_LOGIN_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isOk());
    }

    private ResultActions protectedRequest(String token) throws Exception {
        return mvc.perform(get(AUTH_ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private UUID createTestAccount(String email, boolean enabled, UserRole... roles) {
        UUID id = UUID.randomUUID();

        database.update(
            "insert into user_account(id, email, password_hash, enabled) values (?, ?, ?, ?)",
            id,
            email,
            passwordEncoder.encode(PASSWORD),
            enabled
        );

        database.update(
            "insert into user_profile(user_id, display_name) values (?, 'Test')",
            id
        );

        for (UserRole role : roles) {
            database.update(
                "insert into user_roles(user_id, role) values (?, cast(? as user_role))",
                id,
                role.name()
            );
        }

        return id;
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post(AUTH_LOGIN_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))));
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
        @GetMapping(TEST_ATHLETE_ENDPOINT)
        String athleteProbe() {
            return "ok";
        }

        @TrainerAuthority
        @GetMapping(TEST_TRAINER_ENDPOINT)
        String trainerProbe() {
            return "ok";
        }

        @AdminAuthority
        @GetMapping(TEST_ADMIN_ENDPOINT)
        String adminProbe() {
            return "ok";
        }
    }
}
