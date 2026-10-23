package ru.nsu.crossfitbuddy.core.service;

import jakarta.mail.MessagingException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.nsu.crossfitbuddy.core.dto.request.PasswordResetConfirmation;
import ru.nsu.crossfitbuddy.core.repository.PasswordResetCodeRepository;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mail.MailService;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
public class PasswordRecoveryService {
    private static final Duration CODE_TTL = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private final UserAccountRepository accounts;
    private final PasswordResetCodeRepository codes;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final SecureRandom random = new SecureRandom();

    public PasswordRecoveryService(UserAccountRepository accounts, PasswordResetCodeRepository codes,
        PasswordEncoder encoder, MailService mail, Clock clock, PlatformTransactionManager transactionManager) {
        this.accounts = accounts;
        this.codes = codes;
        this.encoder = encoder;
        this.mail = mail;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public void requestCode(String email) {
        transactions.executeWithoutResult(transaction -> {
            var account = accounts.findByEmailForUpdate(AccountDetailsService.normalizeEmail(email)).orElse(null);
            if (account == null || !account.isEnabled() || account.isDeleted()) {
                return;
            }

            var previous = codes.findLatest(account.getId()).orElse(null);
            String code;
            do {
                code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            } while (previous != null && encoder.matches(code, previous.hash()));

            var now = clock.instant();
            codes.replace(account.getId(), encoder.encode(code), now, now.plus(CODE_TTL));

            try {
                mail.sendHtml(account.getEmail(), "Восстановление доступа к системе",
                    "Для восстановления пароля используйте код: " + code
                        + ". Код действителен в течение 15 минут");
            } catch (MessagingException | MailException exception) {
                transaction.setRollbackOnly();
                log.error("Password recovery email delivery failed: {}", exception.getClass().getSimpleName());
            }
        });
    }

    public void resetPassword(PasswordResetConfirmation request) {
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Passwords do not match.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG",
                "Password must not exceed 72 UTF-8 bytes.");
        }

        var failure = transactions.execute(transaction -> {
            var account = accounts.findByEmailForUpdate(AccountDetailsService.normalizeEmail(request.email()))
                .orElse(null);
            if (account == null || !account.isEnabled() || account.isDeleted()) {
                return Failure.INVALID;
            }

            var code = codes.findLatest(account.getId()).orElse(null);
            if (code == null || code.consumed()) {
                return Failure.INVALID;
            }
            if (code.failedAttempts() >= MAX_ATTEMPTS) {
                return Failure.EXHAUSTED;
            }
            if (!clock.instant().isBefore(code.expiresAt())) {
                return Failure.EXPIRED;
            }
            if (!encoder.matches(request.code(), code.hash())) {
                codes.recordFailure(code.id());
                return code.failedAttempts() + 1 >= MAX_ATTEMPTS ? Failure.EXHAUSTED : Failure.INVALID;
            }

            account.resetPassword(encoder.encode(request.password()));
            codes.consume(code.id(), clock.instant());
            return null;
        });

        if (failure != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, failure.code, failure.message);
        }
    }

    private enum Failure {
        INVALID("INVALID_RESET_CODE", "Неверный код восстановления"),
        EXPIRED("RESET_CODE_EXPIRED", "Срок действия кода истек. Запросите новый код"),
        EXHAUSTED("RESET_ATTEMPTS_EXCEEDED", "Превышено количество попыток. Запросите новый код");

        private final String code;
        private final String message;

        Failure(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
