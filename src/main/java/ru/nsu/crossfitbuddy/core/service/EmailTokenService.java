package ru.nsu.crossfitbuddy.core.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.entity.EmailToken;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mail.service.MailCreator;
import ru.nsu.crossfitbuddy.core.mail.service.MailService;
import ru.nsu.crossfitbuddy.core.repository.EmailTokenRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class EmailTokenService {
    private final EmailTokenRepository tokenRepository;

    private final UserAccountRepository userAccountRepository;

    private final MailService mailService;
    private final MailCreator mailCreator;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.frontend.email-confirmation-path}")
    private String emailConfirmationPath;

    @Transactional
    public void sendConfirmationEmail(UserAccount userAccount) {
        if (userAccount.isEnabled()) {
            throw createApiException(HttpStatus.BAD_REQUEST);
        }

        String token = UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        tokenRepository.invalidateOldTokens(userAccount.getId());

        var expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

        EmailToken emailToken = EmailToken.builder()
                .userAccountId(userAccount.getId())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        tokenRepository.save(emailToken);

        String confirmationLink = generateConfirmationLink(token);
        var expiresAtLocalTime = LocalDateTime.ofInstant(expiresAt, ZoneId.of("Europe/Moscow"));

        try {
            mailService.sendHtml(mailCreator.getEmailConfirmMessage(expiresAtLocalTime,
                    confirmationLink, userAccount.getEmail()));
        } catch (MessagingException e) {
            throw createApiException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void confirmEmail(String token) {
        String hash = hashToken(token);
        EmailToken emailToken = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> createApiException(HttpStatus.BAD_REQUEST));

        if (emailToken.getUsedAt() != null) {
            throw createApiException(HttpStatus.GONE);
        }
        if (emailToken.getExpiresAt().isBefore(Instant.now())) {
            throw createApiException(HttpStatus.GONE);
        }

        UserAccount user = userAccountRepository.findById(emailToken.getUserAccountId())
                .orElseThrow(() -> createApiException(HttpStatus.NOT_FOUND));

        user.setEnabled(true);
        userAccountRepository.save(user);

        emailToken.setUsedAt(Instant.now());
        tokenRepository.save(emailToken);
    }

    @Transactional
    public void resendEmail(String email) {
        UserAccount user = userAccountRepository
                .findByEmail(AccountDetailsService.normalizeEmail(email))
                .orElseThrow(() -> createApiException(HttpStatus.NOT_FOUND));

        sendConfirmationEmail(user);
    }

    private String generateConfirmationLink(String token) {
        return frontendBaseUrl + emailConfirmationPath + "?token=" + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw createApiException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ApiException createApiException(HttpStatus httpStatus) {
        return new ApiException(httpStatus, "INVALID_TOKEN", "Ошибка проверки токена");
    }
}
