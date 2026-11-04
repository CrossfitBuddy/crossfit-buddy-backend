package ru.nsu.crossfitbuddy.core.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTokenService {
    private static final String FRONTEND_EMAIL_ROUTE = "/auth/email-confirmation";

    private final EmailTokenRepository tokenRepository;

    private final UserAccountRepository userAccountRepository;

    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public void sendConfirmationEmail(UserAccount userAccount) {
        if (userAccount.isEnabled()) {
            throw createApiException(HttpStatus.BAD_REQUEST);
        }

        String token = UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        tokenRepository.invalidateOldTokens(userAccount.getId());

        EmailToken emailToken = EmailToken.builder()
            .userAccountId(userAccount.getId())
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .build();
        tokenRepository.save(emailToken);

        String confirmationLink = generateConfirmationLink(token);
        try {
            mailService.sendHtml(userAccount.getEmail(), "Подтверждение регистрации", confirmationLink);
        }
        catch (MessagingException e) {
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
        UserAccount user = userAccountRepository.findByEmail(email)
            .orElseThrow(() -> createApiException(HttpStatus.NOT_FOUND));

        sendConfirmationEmail(user);
    }

    private String generateConfirmationLink(String token) {
        return frontendUrl + FRONTEND_EMAIL_ROUTE + "?token=" + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw createApiException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ApiException createApiException(HttpStatus httpStatus) {
        return new ApiException(httpStatus, "INVALID_TOKEN", "Ошибка проверки токена");
    }
}
