package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.user.service.AvatarService;
import ru.nsu.crossfitbuddy.core.user.service.AvatarValidator;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users/me/avatar")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AvatarController {
    private final AvatarService avatarService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> replaceCurrentAvatar(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestPart("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "File must not be empty.");
        }
        if (file.getSize() > AvatarValidator.MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE",
                "Avatar must not exceed 5 MB.");
        }

        avatarService.replaceCurrentAvatar(currentUser, file);

        return ResponseEntity.noContent()
            .cacheControl(CacheControl.noStore())
            .build();
    }

    @GetMapping
    public ResponseEntity<byte[]> getCurrentAvatar(@AuthenticationPrincipal CurrentUser currentUser) {
        var avatar = avatarService.getCurrentAvatar(currentUser);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(avatar.contentType()))
            .body(avatar.content());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCurrentAvatar(@AuthenticationPrincipal CurrentUser currentUser) {
        avatarService.deleteCurrentAvatar(currentUser);

        return ResponseEntity.noContent()
            .cacheControl(CacheControl.noStore())
            .build();
    }
}
