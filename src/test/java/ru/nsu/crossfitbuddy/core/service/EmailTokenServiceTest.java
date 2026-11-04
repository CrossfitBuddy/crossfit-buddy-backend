package ru.nsu.crossfitbuddy.core.service;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import ru.nsu.crossfitbuddy.core.entity.EmailToken;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mail.MailService;
import ru.nsu.crossfitbuddy.core.repository.EmailTokenRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTokenServiceTest {

    private static final String FRONTEND_URL = "http://localhost:5173";
    private static final String CONFIRM_SUBJECT = "Подтверждение регистрации";

    @Mock
    private EmailTokenRepository tokenRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailTokenService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            tokenRepository,
            userAccountRepository,
            mailService
        );
    }

    @Test
    void sendConfirmationEmail_throwsBadRequest_ifUserAlreadyEnabled() {
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);

        assertThatThrownBy(() -> service.sendConfirmationEmail(user))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void sendConfirmationEmail_invalidatesOldTokens_savesNewToken_andSendsLink() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(false);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(email);

        service.sendConfirmationEmail(user);

        ArgumentCaptor<EmailToken> tokenCaptor = ArgumentCaptor.forClass(EmailToken.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        verify(tokenRepository).invalidateOldTokens(userId);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(mailService).sendHtml(eq(email), eq(CONFIRM_SUBJECT), linkCaptor.capture());

        EmailToken savedToken = tokenCaptor.getValue();
        String link = linkCaptor.getValue();

        assertThat(savedToken.getUserAccountId()).isEqualTo(userId);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getUsedAt()).isNull();
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());

        assertThat(link).startsWith(FRONTEND_URL + "/auth/email-confirmation?token=");

        String rawToken = extractTokenFromLink(link);
        assertThat(hashToken(rawToken)).isEqualTo(savedToken.getTokenHash());
    }

    @Test
    void sendConfirmationEmail_throwsInternalServerError_ifMailFails() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(false);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(email);

        doThrow(new MessagingException("SMTP down"))
            .when(mailService)
            .sendHtml(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.sendConfirmationEmail(user))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));

        verify(tokenRepository).invalidateOldTokens(userId);
        verify(tokenRepository).save(any(EmailToken.class));
        verify(mailService).sendHtml(eq(email), eq(CONFIRM_SUBJECT), anyString());
    }

    @Test
    void confirmEmail_throwsBadRequest_ifTokenNotFound() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmEmail("raw-token"))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void confirmEmail_throwsGone_ifTokenAlreadyUsed() {
        EmailToken token = EmailToken.builder()
            .userAccountId(UUID.randomUUID())
            .tokenHash("hash")
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .usedAt(Instant.now().minusSeconds(1))
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmEmail("raw-token"))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void confirmEmail_throwsGone_ifTokenExpired() {
        EmailToken token = EmailToken.builder()
            .userAccountId(UUID.randomUUID())
            .tokenHash("hash")
            .expiresAt(Instant.now().minusSeconds(1))
            .usedAt(null)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmEmail("raw-token"))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void confirmEmail_throwsNotFound_ifUserMissing() {
        UUID userId = UUID.randomUUID();

        EmailToken token = EmailToken.builder()
            .userAccountId(userId)
            .tokenHash("hash")
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .usedAt(null)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmEmail("raw-token"))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void confirmEmail_enablesUser_andMarksTokenUsed() {
        UUID userId = UUID.randomUUID();

        EmailToken token = EmailToken.builder()
            .userAccountId(userId)
            .tokenHash("hash")
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .usedAt(null)
            .build();

        UserAccount user = mock(UserAccount.class);

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

        service.confirmEmail("raw-token");

        verify(user).setEnabled(true);
        verify(userAccountRepository).save(user);
        verify(tokenRepository).save(token);

        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void resendEmail_throwsBadRequest_ifUserAlreadyEnabled() {
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);

        when(userAccountRepository.findByEmail("enabled@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resendEmail("enabled@example.com"))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).status())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void resendEmail_sendsNewConfirmationEmail_ifUserNotEnabled() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(false);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(email);

        when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user));

        service.resendEmail(email);

        ArgumentCaptor<EmailToken> tokenCaptor = ArgumentCaptor.forClass(EmailToken.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        verify(tokenRepository).invalidateOldTokens(userId);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(mailService).sendHtml(eq(email), eq(CONFIRM_SUBJECT), linkCaptor.capture());

        EmailToken savedToken = tokenCaptor.getValue();
        String link = linkCaptor.getValue();

        assertThat(savedToken.getUserAccountId()).isEqualTo(userId);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getUsedAt()).isNull();
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());

        assertThat(link).startsWith(FRONTEND_URL + "/auth/email-confirmation?token=");

        String rawToken = extractTokenFromLink(link);
        assertThat(hashToken(rawToken)).isEqualTo(savedToken.getTokenHash());
    }

    private String extractTokenFromLink(String link) {
        String marker = "?token=";
        int index = link.indexOf(marker);
        if (index < 0) {
            throw new IllegalArgumentException("Token not found in link: " + link);
        }
        return link.substring(index + marker.length());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
