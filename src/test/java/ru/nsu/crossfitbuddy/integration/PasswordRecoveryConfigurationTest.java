package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ru.nsu.crossfitbuddy.core.dto.request.PasswordResetConfirmation;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.RawMailTemplate;
import ru.nsu.crossfitbuddy.core.mail.mailTemplates.PasswordRecoveryMailTemlates;
import ru.nsu.crossfitbuddy.core.mail.service.MailService;
import ru.nsu.crossfitbuddy.core.repository.AbstractRepositoryTest;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.PasswordRecoveryService;

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
        var account = accounts.save(UserAccount.builder()
                .email(email)
                .passwordHash("old-hash")
                .enabled(true)
                .build());
        PasswordRecoveryMailTemlates[] delivered = new PasswordRecoveryMailTemlates[1];

        ArgumentCaptor<PasswordRecoveryMailTemlates> mailTemplateCaptor = ArgumentCaptor
                .forClass(PasswordRecoveryMailTemlates.class);
        doAnswer(invocation -> {
            delivered[0] = (invocation.getArgument(0, PasswordRecoveryMailTemlates.class));
            return null;
        }).when(mailService).sendHtml(mailTemplateCaptor.capture());

        service.requestCode(email);

        assertEquals(email, mailTemplateCaptor.getValue().getToAddress());

        assertThat(database.queryForObject(
                "select extract(epoch from expires_at-created_at)::int from password_reset_code where user_id=?",
                Integer.class, account.getId())).isEqualTo(1200);
        var matcher = Pattern.compile("[0-9]{6}")
                .matcher(Jsoup.parse(delivered[0].getBody()).text());
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
