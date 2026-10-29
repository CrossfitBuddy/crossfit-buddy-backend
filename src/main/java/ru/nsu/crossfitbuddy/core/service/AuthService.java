package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.nsu.crossfitbuddy.core.dto.response.AccountResponse;
import ru.nsu.crossfitbuddy.core.dto.response.AuthResponse;
import ru.nsu.crossfitbuddy.core.dto.request.LoginRequest;
import ru.nsu.crossfitbuddy.core.dto.request.RegisterRequest;
import ru.nsu.crossfitbuddy.core.dto.response.RegisterResponse;
import ru.nsu.crossfitbuddy.core.entity.AccountDetails;
import ru.nsu.crossfitbuddy.core.entity.AthleteProfile;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.repository.AthleteProfileRepository;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.repository.UserProfileRepository;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final AthleteProfileRepository athleteProfileRepository;
    private final JwtService jwtService;
    private final EmailTokenService emailTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        validatePasswordByteLength(request.password());
        var authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                .unauthenticated(AccountDetailsService.normalizeEmail(request.email()), request.password()));
        var account = ((AccountDetails) authentication.getPrincipal()).account();
        UserRole role = resolveInitialActiveRole(account);
        account.selectActiveRole(role);
        var response = createAuthResponse(account, role);
        log.info("User authenticated, login response prepared: userId={}, activeRole={}", account.getId(), role);
        return response;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        validatePasswordByteLength(request.password());
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Passwords do not match.");
        }

        String email = AccountDetailsService.normalizeEmail(request.email());
        if (userAccountRepository.existsByEmail(email)) {
            throw emailConflict();
        }

        String firstName = request.firstName().strip();
        String lastName = request.lastName().strip();

        var account = UserAccount.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .roles(new HashSet<>(Set.of(UserRole.ATHLETE)))
            .activeRole(UserRole.ATHLETE)
            .enabled(false)
            .build();

        try {
            userAccountRepository.saveAndFlush(account);
        }
        catch (DataIntegrityViolationException exception) {
            if ("ux_user_account_email_normalized".equals(constraintName(exception))
                || "user_account_email_key".equals(constraintName(exception))) {
                throw emailConflict();
            }
            throw exception;
        }

        userProfileRepository.save(UserProfile.create(account, firstName, lastName));
        athleteProfileRepository.save(AthleteProfile.create(account));

        emailTokenService.sendConfirmationEmail(account);

        log.info("Registration completed, confirmation email sent: userId={}", account.getId());
        return new RegisterResponse(account.getId(), "Проверьте вашу почту для подтверждения регистрации");
    }

    @Transactional(readOnly = true)
    public AccountResponse getCurrentAccount(CurrentUser currentUser) {
        var account = findCurrentAccountOrThrow(currentUser);
        return AccountResponse.from(account, displayName(account), UserRole.valueOf(currentUser.activeRole()));
    }

    @Transactional
    public AuthResponse switchActiveRole(CurrentUser currentUser, UserRole role) {
        var account = findCurrentAccountOrThrow(currentUser);
        if (!account.getRoles().contains(role) || (account.getRoles().contains(UserRole.ADMIN)
            && role != UserRole.ADMIN)) {
            log.warn("Active role change denied: userId={}, requestedRole={}", account.getId(), role);
            throw new AccessDeniedException("Requested role is not assigned to this account");
        }
        account.selectActiveRole(role);
        var response = createAuthResponse(account, role);
        log.info("Active role change response prepared, transaction commit pending: userId={}, activeRole={}",
            account.getId(), role);
        return response;
    }

    private UserAccount findCurrentAccountOrThrow(CurrentUser currentUser) {
        return userAccountRepository.findById(currentUser.id())
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    }

    static UserRole resolveInitialActiveRole(UserAccount user) {
        if (user.getRoles().contains(UserRole.ADMIN)) {
            return UserRole.ADMIN;
        }
        if (user.getActiveRole() != null && user.getRoles().contains(user.getActiveRole())) {
            return user.getActiveRole();
        }
        if (user.getRoles().contains(UserRole.ATHLETE)) {
            return UserRole.ATHLETE;
        }
        if (user.getRoles().contains(UserRole.TRAINER)) {
            return UserRole.TRAINER;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_NOT_ASSIGNED",
            "No role is assigned to this account. Contact an administrator.");
    }

    private AuthResponse createAuthResponse(UserAccount user, UserRole role) {
        var token = jwtService.issue(user, role);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), AccountResponse
            .from(user, displayName(user), role));
    }

    private String displayName(UserAccount user) {
        return userProfileRepository.findById(user.getId())
            .map(UserProfile::getDisplayName)
            .filter(name -> !name.isBlank())
            .orElse(user.getEmail());
    }

    private static void validatePasswordByteLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG",
                "Password must not exceed 72 UTF-8 bytes.");
        }
    }

    private static ApiException emailConflict() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
            "An account with this email already exists.");
    }

    private static String constraintName(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
