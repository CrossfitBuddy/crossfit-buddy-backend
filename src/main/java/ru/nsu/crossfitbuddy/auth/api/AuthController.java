package ru.nsu.crossfitbuddy.auth.api;

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

import ru.nsu.crossfitbuddy.auth.service.AuthService;
import ru.nsu.crossfitbuddy.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return response(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return response(HttpStatus.CREATED, authService.register(request));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AccountResponse> getCurrentAccount(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(authService.getCurrentAccount(currentUser));
    }

    @PatchMapping("/active-role")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthResponse> switchActiveRole(@AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody ActiveRoleRequest request) {
        return response(HttpStatus.OK, authService.switchActiveRole(currentUser, request.role()));
    }

    private static ResponseEntity<AuthResponse> response(HttpStatus status, AuthResponse body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
