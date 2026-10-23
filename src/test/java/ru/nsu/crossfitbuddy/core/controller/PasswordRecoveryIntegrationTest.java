package ru.nsu.crossfitbuddy.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import jakarta.mail.Multipart;
import jakarta.mail.Part;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.nsu.crossfitbuddy.core.mail.SMTPMailService;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
class PasswordRecoveryIntegrationTest extends AbstractRepositoryTest {
    private static final String REQUEST = "/api/v1/auth/password/request";
    private static final String CONFIRM = "/api/v1/auth/password/confirm";
    private static final String OLD_PASSWORD = "OldStrongPassword123";
    private static final String NEW_PASSWORD = "New1!abc";
    private static final GreenMail SMTP = new GreenMail(new ServerSetup(0, "127.0.0.1", "smtp"));

    static {
        SMTP.start();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate database;

    @Autowired
    UserAccountRepository accounts;

    @Autowired
    PasswordEncoder encoder;

    @MockitoBean
    Clock clock;

    @MockitoSpyBean
    SMTPMailService mail;

    private Instant now;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> SMTP.getSmtp().getPort());
    }

    @BeforeEach
    void setUp() throws Exception {
        now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(now);
        SMTP.purgeEmailFromAllMailboxes();
    }

    @AfterAll
    static void stopMailServer() {
        SMTP.stop();
    }

    @Test
    @DisplayName("Смена пароля и проверка старого JWT")
    void resetsPasswordAndInvalidatesOldCredentials() throws Exception {
        var user = register();
        requestCode(" " + user.email().toUpperCase(Locale.ROOT) + " ")
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        String code = deliveredCode();
        var row = database.queryForMap("select * from password_reset_code where user_id=?", user.id());
        assertThat(encoder.matches(code, (String) row.get("code_hash"))).isTrue();
        assertThat(row.get("code_hash")).isNotEqualTo(code);
        assertThat(database.queryForObject(
            "select extract(epoch from expires_at-created_at)::int from password_reset_code where user_id=?",
            Integer.class, user.id())).isEqualTo(900);

        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("message").isNotEmpty())
            .andExpect(jsonPath("accessToken").doesNotExist());
        var account = accounts.findById(user.id()).orElseThrow();
        assertThat(encoder.matches(NEW_PASSWORD, account.getPasswordHash())).isTrue();
        assertThat(account.getTokenVersion()).isEqualTo(1);
        assertThat(database.queryForObject("select consumed_at is not null from password_reset_code where user_id=?",
            Boolean.class, user.id())).isTrue();
        login(user.email(), OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(user.email(), NEW_PASSWORD).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token()))
            .andExpect(status().isUnauthorized());
        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_RESET_CODE"));
        login(user.email(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Одинаковый ответ для неизвестных и недоступных аккаунтов")
    void doesNotDiscloseAccountExistenceOrState() throws Exception {
        var user = register();
        String expected = requestCode(user.email()).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        int count = SMTP.getReceivedMessages().length;
        assertThat(requestCode(UUID.randomUUID() + "@example.com").andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).isEqualTo(expected);
        database.update("update user_account set enabled=false where id=?", user.id());
        assertThat(requestCode(user.email()).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).isEqualTo(expected);
        database.update("update user_account set enabled=true, deleted=true where id=?", user.id());
        assertThat(requestCode(user.email()).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).isEqualTo(expected);
        assertThat(SMTP.getReceivedMessages()).hasSize(count);
        confirm(user.email(), deliveredCode(), NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_RESET_CODE"));
        confirm("unknown@example.com", "000000", NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_RESET_CODE"));
    }

    @Test
    @DisplayName("Лимит попыток и запрос нового кода")
    void persistsAttemptsAndReplacesExhaustedCode() throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String code = deliveredCode();
        String wrong = wrongCode(code);
        for (int attempt = 1; attempt <= 5; attempt++) {
            confirm(user.email(), wrong, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value(attempt == 5 ? "RESET_ATTEMPTS_EXCEEDED" : "INVALID_RESET_CODE"));
            assertThat(failedAttempts(user.id())).isEqualTo(attempt);
        }
        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("RESET_ATTEMPTS_EXCEEDED"));
        assertThat(failedAttempts(user.id())).isEqualTo(5);
        requestCode(user.email()).andExpect(status().isOk());
        String replacement = deliveredCode();
        assertThat(replacement).isNotEqualTo(code);
        assertThat(failedAttempts(user.id())).isZero();
        confirm(user.email(), replacement, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Новый код заменяет предыдущий")
    void resendInvalidatesPreviousCode() throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String previous = deliveredCode();
        requestCode(user.email()).andExpect(status().isOk());
        String current = deliveredCode();
        assertThat(current).isNotEqualTo(previous);
        assertThat(database.queryForObject("select count(*) from password_reset_code where user_id=?",
            Integer.class, user.id())).isEqualTo(1);
        confirm(user.email(), previous, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_RESET_CODE"));
        confirm(user.email(), current, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Код истекает через 15 минут")
    void expiresAtTtlBoundary() throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String code = deliveredCode();
        when(clock.instant()).thenReturn(now.plus(Duration.ofMinutes(15)));
        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("RESET_CODE_EXPIRED"));
        assertThat(failedAttempts(user.id())).isZero();
        login(user.email(), OLD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ошибка отправки сохраняет старый код")
    void mailFailureRollsBackReplacement() throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String code = deliveredCode();
        doThrow(new MailSendException("SMTP unavailable")).when(mail).sendHtml(anyString(), anyString(), anyString());
        requestCode(user.email()).andExpect(status().isOk())
            .andExpect(jsonPath("message").value("Код восстановления отправлен на указанный email"));
        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Код можно использовать только один раз")
    void consumesCodeExactlyOnce() throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String code = deliveredCode();
        var first = CompletableFuture.supplyAsync(() -> confirmStatus(user.email(), code));
        var second = CompletableFuture.supplyAsync(() -> confirmStatus(user.email(), code));
        assertThat(List.of(first.join(), second.join())).containsExactlyInAnyOrder(200, 400);
        assertThat(accounts.findById(user.id()).orElseThrow().getTokenVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Лимит попыток при одновременных запросах")
    void serializesFailedAttempts() throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String code = deliveredCode();
        var requests = IntStream.range(0, 7)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> confirmStatus(user.email(), wrongCode(code))))
            .toList();
        assertThat(requests.stream().map(CompletableFuture::join).toList()).containsOnly(400);
        assertThat(failedAttempts(user.id())).isEqualTo(5);
        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("RESET_ATTEMPTS_EXCEEDED"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPasswords")
    @DisplayName("Проверка нового пароля")
    void validatesPassword(String name, String password, String confirmation, String error) throws Exception {
        var user = register();
        requestCode(user.email()).andExpect(status().isOk());
        String code = deliveredCode();
        confirm(user.email(), code, password, confirmation)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("status").value(400))
            .andExpect(jsonPath("code").value(error))
            .andExpect(jsonPath("timestamp").isNotEmpty())
            .andExpect(jsonPath("message").isNotEmpty())
            .andExpect(jsonPath("path").value(CONFIRM));
        assertThat(failedAttempts(user.id())).isZero();
        assertThat(encoder.matches(OLD_PASSWORD,
            accounts.findById(user.id()).orElseThrow().getPasswordHash())).isTrue();
        confirm(user.email(), code, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidPasswords() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("Short", "Ab1!xyz", "Ab1!xyz", "VALIDATION_ERROR"),
            org.junit.jupiter.params.provider.Arguments.of("No digits", "LettersOnly!Password", "LettersOnly!Password",
                "VALIDATION_ERROR"),
            org.junit.jupiter.params.provider.Arguments.of("No letters", "123456789!", "123456789!",
                "VALIDATION_ERROR"),
            org.junit.jupiter.params.provider.Arguments.of("No special characters", "LettersOnlyPassword123",
                "LettersOnlyPassword123",
                "VALIDATION_ERROR"),
            org.junit.jupiter.params.provider.Arguments.of("Mismatch", NEW_PASSWORD, "Other1!abc",
                "PASSWORD_MISMATCH"),
            org.junit.jupiter.params.provider.Arguments.of("UTF-8 bytes", "界".repeat(24) + "1!",
                "界".repeat(24) + "1!",
                "PASSWORD_TOO_LONG"));
    }

    @Test
    @DisplayName("Проверка запросов восстановления")
    void validatesPublicRequests() throws Exception {
        mvc.perform(post(REQUEST).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalid\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("VALIDATION_ERROR"));
        var user = register();
        mvc.perform(post(REQUEST).header(HttpHeaders.AUTHORIZATION, "Bearer expired")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", user.email()))))
            .andExpect(status().isOk());
        String code = deliveredCode();
        for (String invalid : List.of("12345", "1234567", "abcdef", "１２３４５６")) {
            confirm(user.email(), invalid, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
        }
        mvc.perform(post(CONFIRM).header(HttpHeaders.AUTHORIZATION, "Bearer expired")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "email", user.email(), "code", code, "password", NEW_PASSWORD,
                "passwordConfirmation", NEW_PASSWORD))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Код подходит только своему аккаунту")
    void bindsCodeToAccount() throws Exception {
        var owner = register();
        var other = register();
        requestCode(owner.email()).andExpect(status().isOk());
        String code = deliveredCode();
        confirm(other.email(), code, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_RESET_CODE"));
        when(clock.instant()).thenReturn(now.plus(Duration.ofMinutes(15)).minusSeconds(1));
        confirm(owner.email(), code, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        assertThat(encoder.matches(OLD_PASSWORD,
            accounts.findById(other.id()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Повторная отправка при одновременных запросах")
    void serializesResends() throws Exception {
        var user = register();
        var first = CompletableFuture.supplyAsync(() -> requestStatus(user.email()));
        var second = CompletableFuture.supplyAsync(() -> requestStatus(user.email()));
        assertThat(List.of(first.join(), second.join())).containsOnly(200);
        assertThat(database.queryForObject("select count(*) from password_reset_code where user_id=?",
            Integer.class, user.id())).isEqualTo(1);
        assertThat(SMTP.getReceivedMessages()).hasSize(2);
        confirm(user.email(), deliveredCode(), NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
    }

    private int requestStatus(String email) {
        try {
            return requestCode(email).andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private User register() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        var response = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "firstName", "Иван", "lastName", "Иванов",
                "password", OLD_PASSWORD, "passwordConfirmation", OLD_PASSWORD))))
            .andExpect(status().isCreated()).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());
        return new User(UUID.fromString(body.path("user").path("id").asText()),
            email, body.path("accessToken").asText());
    }

    private ResultActions requestCode(String email) throws Exception {
        return mvc.perform(post(REQUEST).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions confirm(String email, String code, String password, String confirmation) throws Exception {
        return mvc.perform(post(CONFIRM).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "code", code, "password", password,
                "passwordConfirmation", confirmation))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private int confirmStatus(String email, String code) {
        try {
            return confirm(email, code, NEW_PASSWORD, NEW_PASSWORD).andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int failedAttempts(UUID userId) {
        return database.queryForObject("select failed_attempts from password_reset_code where user_id=?",
            Integer.class, userId);
    }

    private String deliveredCode() throws Exception {
        var messages = SMTP.getReceivedMessages();
        assertThat(messages).isNotEmpty();
        var message = messages[messages.length - 1];
        assertThat(message.getSubject()).isEqualTo("Восстановление доступа к системе");
        var matcher = Pattern.compile("[0-9]{6}").matcher(text(message));
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private String text(Part part) throws Exception {
        if (part.getContent() instanceof Multipart multipart) {
            var result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                result.append(text(multipart.getBodyPart(i)));
            }
            return result.toString();
        }
        return part.getContent().toString();
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }

    private record User(UUID id, String email, String token) {
    }
}
