package ru.nsu.crossfitbuddy.core.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.nsu.crossfitbuddy.core.dto.request.PasswordResetConfirmation;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mail.MailService;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.PasswordRecoveryService;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
    "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.password-recovery.code-ttl=20m",
    "app.password-recovery.max-attempts=6"
})
class PasswordRecoveryConfigurationTest extends AbstractRepositoryTest {
    @Autowired
    PasswordRecoveryService service;

    @Autowired
    UserAccountRepository accounts;

    @Autowired
    JdbcTemplate database;

    @MockitoBean
    MailService mailService;

    @Test
    void usesConfiguredTtlAndAttemptLimit() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        var account = accounts.save(UserAccount.builder().email(email).passwordHash("old-hash").build());
        var delivered = new StringBuilder();
        doAnswer(invocation -> {
            delivered.append(invocation.getArgument(2, String.class));
            return null;
        }).when(mailService).sendHtml(eq(email), anyString(), anyString());

        service.requestCode(email);

        assertThat(database.queryForObject(
            "select extract(epoch from expires_at-created_at)::int from password_reset_code where user_id=?",
            Integer.class, account.getId())).isEqualTo(1200);
        assertThat(delivered).contains("1200 секунд");
        var matcher = Pattern.compile("[0-9]{6}").matcher(delivered);
        assertThat(matcher.find()).isTrue();
        String code = matcher.group();
        String wrongCode = code.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.resetPassword(
                new PasswordResetConfirmation(email, wrongCode, "New1!abc", "New1!abc")))
                .isInstanceOf(ApiException.class);
        }
        service.resetPassword(new PasswordResetConfirmation(email, code, "New1!abc", "New1!abc"));
        assertThat(accounts.findById(account.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
    }
}
