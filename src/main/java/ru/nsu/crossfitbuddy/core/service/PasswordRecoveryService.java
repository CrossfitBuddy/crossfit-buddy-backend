package ru.nsu.crossfitbuddy.core.service;

import jakarta.mail.MessagingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ru.nsu.crossfitbuddy.config.PasswordRecoveryProperties;
import ru.nsu.crossfitbuddy.core.dto.request.PasswordResetConfirmation;
import ru.nsu.crossfitbuddy.core.entity.PasswordResetCode;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mail.MailService;
import ru.nsu.crossfitbuddy.core.repository.PasswordResetCodeRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final PasswordRecoveryProperties properties;

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final SecureRandom random = new SecureRandom();

    public void requestCode(String email) {
        transactionTemplate.executeWithoutResult(transaction -> {
            var result = userAccountRepository.findByEmailForUpdate(AccountDetailsService.normalizeEmail(email));
            if (result.isEmpty()) {
                return;
            }
            var account = result.get();
            if (!account.isEnabled() || account.isDeleted()) {
                return;
            }

            var previous = passwordResetCodeRepository.findByUserAccountId(account.getId()).orElse(null);
            String code;
            do {
                code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            } while (previous != null && passwordEncoder.matches(code, previous.getCodeHash()));

            var now = clock.instant();
            var resetCode = previous == null ? new PasswordResetCode(account) : previous;
            resetCode.replace(passwordEncoder.encode(code), now, now.plus(properties.codeTtl()));
            passwordResetCodeRepository.save(resetCode);

            try {
                mailService.sendHtml(account.getEmail(), "Восстановление доступа к системе",
                    "Для восстановления пароля используйте код: " + code
                        + ". Срок действия кода: " + properties.codeTtl().toSeconds() + " секунд");
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

        var failure = transactionTemplate.execute(transaction -> {
            var result = userAccountRepository.findByEmailForUpdate(
                AccountDetailsService.normalizeEmail(request.email()));
            if (result.isEmpty()) {
                return PasswordRecoveryFailure.INVALID;
            }
            var account = result.get();
            if (!account.isEnabled() || account.isDeleted()) {
                return PasswordRecoveryFailure.INVALID;
            }

            var code = passwordResetCodeRepository.findByUserAccountId(account.getId()).orElse(null);
            if (code == null || code.getConsumedAt() != null) {
                return PasswordRecoveryFailure.INVALID;
            }
            if (code.getFailedAttempts() >= properties.maxAttempts()) {
                return PasswordRecoveryFailure.EXHAUSTED;
            }
            if (!clock.instant().isBefore(code.getExpiresAt())) {
                return PasswordRecoveryFailure.EXPIRED;
            }
            if (!passwordEncoder.matches(request.code(), code.getCodeHash())) {
                code.recordFailure();
                return code.getFailedAttempts() >= properties.maxAttempts()
                    ? PasswordRecoveryFailure.EXHAUSTED : PasswordRecoveryFailure.INVALID;
            }

            account.resetPassword(passwordEncoder.encode(request.password()));
            code.consume(clock.instant());
            return null;
        });

        if (failure != null) {
            log.debug("Password recovery rejected: {}", failure);
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_CODE",
                "Не удалось восстановить пароль. Проверьте код или запросите новый.");
        }
    }
}
