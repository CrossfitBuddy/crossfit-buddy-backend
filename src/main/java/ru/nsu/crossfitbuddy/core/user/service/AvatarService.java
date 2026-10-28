package ru.nsu.crossfitbuddy.core.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.UserAvatar;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.repository.UserAvatarRepository;

@Service
@RequiredArgsConstructor
public class AvatarService {
    private final ProfileService profileService;
    private final UserAvatarRepository userAvatarRepository;
    private final AvatarValidator avatarValidator;
    private final AvatarStorageService avatarStorageService;

    @Transactional
    public void replaceCurrentAvatar(CurrentUser currentUser, MultipartFile file) {
        profileService.findProfileOrThrow(currentUser.id(), true);

        avatarValidator.validate(file);

        var existingAvatar = userAvatarRepository.findById(currentUser.id()).orElse(null);
        String oldObjectKey = existingAvatar != null ? existingAvatar.getObjectKey() : null;

        String newObjectKey = avatarStorageService.upload(currentUser.id(), file);

        if (existingAvatar != null) {
            existingAvatar.replace(newObjectKey, file.getContentType(), file.getSize());
            userAvatarRepository.save(existingAvatar);

            avatarStorageService.delete(oldObjectKey);
        } else {
            var newAvatar = UserAvatar.create(currentUser.id(), newObjectKey, file.getContentType(), file.getSize());
            userAvatarRepository.save(newAvatar);
        }
    }

    @Transactional(readOnly = true)
    public AvatarContent getCurrentAvatar(CurrentUser currentUser) {
        profileService.findProfileOrThrow(currentUser.id(), false);
        var avatar = userAvatarRepository.findById(currentUser.id())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AVATAR_NOT_FOUND", "Avatar was not found."));

        byte[] content = avatarStorageService.download(avatar.getObjectKey());
        return new AvatarContent(avatar.getContentType(), content);
    }

    @Transactional
    public void deleteCurrentAvatar(CurrentUser currentUser) {
        profileService.findProfileOrThrow(currentUser.id(), true);

        userAvatarRepository.findById(currentUser.id()).ifPresent(avatar -> {
            userAvatarRepository.delete(avatar);
            avatarStorageService.delete(avatar.getObjectKey());
        });
    }

    public record AvatarContent(String contentType, byte[] content) {}
}
