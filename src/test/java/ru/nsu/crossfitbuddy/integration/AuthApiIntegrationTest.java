package ru.nsu.crossfitbuddy.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.nsu.crossfitbuddy.core.controller.Paths;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.AthleteProfileRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.repository.UserProfileRepository;
import ru.nsu.crossfitbuddy.core.service.EmailTokenService;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
class AuthApiIntegrationTest extends AbstractRepositoryTest {
    private static final String PASSWORD = "StrongPassword12345";

    private static final String AUTH_REGISTER_PATH = Paths.AUTH + Paths.AUTH_REGISTER;
    private static final String AUTH_LOGIN_PATH = Paths.AUTH + Paths.AUTH_LOGIN;
    private static final String AUTH_ME_PATH = Paths.AUTH + Paths.AUTH_ME;

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

    @MockitoBean
    EmailTokenService emailTokenService;

    @Test
    void registerCreatesUnconfirmedAccountAndLoginWorksAfterConfirmation() throws Exception {
        String email = generateUniqueEmail();

        var result = register(email, PASSWORD, PASSWORD)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("userId").isString())
            .andExpect(jsonPath("message").isNotEmpty())
            .andExpect(jsonPath("accessToken").doesNotExist())
            .andExpect(jsonPath("user").doesNotExist())
            .andReturn();

        UUID userId = UUID.fromString(
            json.readTree(result.getResponse().getContentAsString()).get("userId").asText()
        );

        verify(emailTokenService).sendConfirmationEmail(
            argThat(account -> account != null && userId.equals(account.getId()))
        );

        var account = userAccountRepository.findById(userId).orElseThrow();

        assertThat(account.isEnabled()).isFalse();
        assertThat(account.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, account.getPasswordHash())).isTrue();
        assertThat(userProfileRepository.findById(account.getId()).orElseThrow().getFirstName()).isEqualTo("Иван");
        assertThat(athleteProfileRepository.findById(account.getId())).isPresent();

        database.update("update user_account set enabled = true where id = ?", userId);

        String token = extractAccessToken(login(email, PASSWORD)
            .andExpect(status().isOk()));

        mvc.perform(get(AUTH_ME_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("email").value(email));
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
            .andExpect(jsonPath("path").value(AUTH_LOGIN_PATH))
            .andExpect(jsonPath("password").doesNotExist());

        register(generateUniqueEmail(), PASSWORD, "DifferentPassword123")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Passwords do not match."));

        mvc.perform(get(AUTH_ME_PATH))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("message").value("Authentication is required or the access token is invalid."));
    }

    @Test
    void returnsExplicitAndDeterministicValidationMessagesRegardlessOfRequestLocale() throws Exception {
        for (String locale : new String[]{"ru", "en"}) {
            mvc.perform(post(AUTH_LOGIN_PATH)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("email", "invalid-email", "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("message").value("email: Enter a valid email address."));
        }

        mvc.perform(post(AUTH_LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", generateUniqueEmail()))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("password: Password is required."));

        var body = registrationBody(generateUniqueEmail(), "", "");
        body.put("firstName", "");
        body.put("lastName", "");

        mvc.perform(post(AUTH_REGISTER_PATH)
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

        login(email, PASSWORD)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesRegistrationAndRejectsClientSuppliedRoles() throws Exception {
        String invalidJson = "{";
        register(generateUniqueEmail(), "short1", "short1")
            .andExpect(status().isBadRequest());

        register(generateUniqueEmail(), "OnlyLettersPassword", "OnlyLettersPassword")
            .andExpect(status().isBadRequest());

        register(generateUniqueEmail(), PASSWORD, "DifferentPassword123")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("PASSWORD_MISMATCH"));

        register("not-email", PASSWORD, PASSWORD)
            .andExpect(status().isBadRequest());

        String longUtf8 = "界".repeat(24) + "123";
        register(generateUniqueEmail(), longUtf8, longUtf8)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("PASSWORD_TOO_LONG"));

        var body = registrationBody(generateUniqueEmail(), PASSWORD, PASSWORD);
        body.put("roles", new String[]{"ADMIN"});

        mvc.perform(post(AUTH_REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value("INVALID_REQUEST"));

        mvc.perform(post(AUTH_LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAmbiguousJsonAndOversizedFieldValues() throws Exception {
        for (String body : new String[]{
            "{\"email\":\"one@example.com\",\"email\":\"two@example.com\",\"password\":\"" + PASSWORD + "\"}",
            "{\"email\":\"one@example.com\",\"password\":\"" + PASSWORD + "\"} {}",
            "{\"email\":\"one@example.com\",\"password\":\"" + "a".repeat(9000) + "\"}"
        }) {
            mvc.perform(post(AUTH_LOGIN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void rejectsCaseInsensitiveDuplicateEmailIncludingConcurrentRegistrations() throws Exception {
        String email = generateUniqueEmail();

        register(email, PASSWORD, PASSWORD)
            .andExpect(status().isCreated());

        register(email.toUpperCase(Locale.ROOT), PASSWORD, PASSWORD)
            .andExpect(status().isConflict());

        String concurrentEmail = generateUniqueEmail();
        var one = CompletableFuture.supplyAsync(() -> registrationStatus(concurrentEmail));
        var two = CompletableFuture.supplyAsync(() -> registrationStatus(concurrentEmail));

        assertThat(List.of(one.join(), two.join()))
            .containsExactlyInAnyOrder(201, 409);

        assertThat(database.queryForObject(
            "select count(*) from user_account where email = ?",
            Long.class,
            concurrentEmail
        )).isEqualTo(1);
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

    private UUID createTestAccount(String email, boolean enabled, UserRole... roles) {
        UUID id = UUID.randomUUID();

        database.update(
            "insert into user_account(id, email, password_hash, enabled) values (?, ?, ?, ?)",
            id,
            email,
            passwordEncoder.encode(PASSWORD),
            enabled
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

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post(AUTH_LOGIN_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private ResultActions register(String email, String password, String confirmation) throws Exception {
        return mvc.perform(post(AUTH_REGISTER_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(registrationBody(email, password, confirmation))));
    }

    private Map<String, Object> registrationBody(String email, String password, String confirmation) {
        return new HashMap<>(Map.of(
            "firstName", "Иван",
            "lastName", "Иванов",
            "email", email,
            "password", password,
            "passwordConfirmation", confirmation,
            "role", UserRole.ATHLETE
        ));
    }

    private int registrationStatus(String email) {
        try {
            return register(email, PASSWORD, PASSWORD)
                .andReturn()
                .getResponse()
                .getStatus();
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
}
