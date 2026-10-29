package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.nsu.crossfitbuddy.core.dto.request.EmailConfirmRequest;
import ru.nsu.crossfitbuddy.core.dto.request.EmailResendRequest;
import ru.nsu.crossfitbuddy.core.dto.response.AccountResponse;
import ru.nsu.crossfitbuddy.core.dto.request.ActiveRoleRequest;
import ru.nsu.crossfitbuddy.core.dto.request.LoginRequest;
import ru.nsu.crossfitbuddy.core.dto.request.RegisterRequest;
import ru.nsu.crossfitbuddy.core.dto.response.AuthResponse;
import ru.nsu.crossfitbuddy.core.dto.response.RegisterResponse;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.service.AuthService;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.service.EmailTokenService;

@RestController
@RequestMapping(Paths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final EmailTokenService tokenService;

    @PostMapping(Paths.AUTH_LOGIN)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return response(HttpStatus.OK, authService.login(request));
    }

    @PostMapping(Paths.AUTH_REGISTER)
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return response(HttpStatus.CREATED, authService.register(request));
    }

    @GetMapping(Paths.AUTH_ME)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AccountResponse> getCurrentAccount(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(authService.getCurrentAccount(currentUser));
    }

    @PatchMapping(Paths.AUTH_ACTIVE_ROLE)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthResponse> switchActiveRole(@AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody ActiveRoleRequest request) {
        return response(HttpStatus.OK, authService.switchActiveRole(currentUser, request.role()));
    }

    @PostMapping(Paths.AUTH_EMAIL_CONFIRM)
    public ResponseEntity<Void> confirm(@RequestBody EmailConfirmRequest request) {
        try {
            tokenService.confirmEmail(request.token());
            return ResponseEntity.ok().build();
        }
        catch (ApiException e) {
            return ResponseEntity.status(e.status()).build();
        }
    }

    @PostMapping(Paths.AUTH_EMAIL_RESEND)
    public ResponseEntity<Void> resend(@RequestBody EmailResendRequest request) {
        try {
            tokenService.resendEmail(request.email());
            return ResponseEntity.ok().build();
        }
        catch (ApiException e) {
            return ResponseEntity.status(e.status()).build();
        }
    }

    private static <T> ResponseEntity<T> response(HttpStatus status, T body) {
        return ResponseEntity.status(status)
            .cacheControl(CacheControl.noStore())
            .body(body);
    }
}
