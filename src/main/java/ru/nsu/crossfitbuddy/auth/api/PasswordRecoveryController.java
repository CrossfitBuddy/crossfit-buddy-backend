package ru.nsu.crossfitbuddy.auth.api;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.nsu.crossfitbuddy.auth.service.PasswordRecoveryService;

@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordRecoveryController {
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/request")
    public ResponseEntity<MessageResponse> request(@Valid @RequestBody PasswordResetRequest request) {
        passwordRecoveryService.requestCode(request.email());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(new MessageResponse("Код восстановления отправлен на указанный email"));
    }

    @PostMapping("/confirm")
    public ResponseEntity<MessageResponse> confirm(@Valid @RequestBody PasswordResetConfirmation request) {
        passwordRecoveryService.resetPassword(request);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(new MessageResponse("Пароль успешно изменен. Вы можете войти в систему с новым паролем"));
    }

    public record MessageResponse(String message) {
    }
}
