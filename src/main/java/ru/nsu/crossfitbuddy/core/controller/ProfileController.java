package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.nsu.crossfitbuddy.core.dto.request.UpdateProfileRequest;
import ru.nsu.crossfitbuddy.core.dto.response.ProfileResponse;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.user.service.ProfileService;

@RestController
@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getCurrentProfile(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(profileService.getCurrentProfile(currentUser));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateCurrentProfile(@AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(profileService.updateCurrentProfile(currentUser, request));
    }
}
