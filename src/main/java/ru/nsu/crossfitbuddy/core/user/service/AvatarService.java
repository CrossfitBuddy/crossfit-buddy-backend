package ru.nsu.crossfitbuddy.core.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.crossfitbuddy.common.api.ApiException;
import ru.nsu.crossfitbuddy.core.user.domain.UserAvatar;
import ru.nsu.crossfitbuddy.core.user.persistence.UserAvatarRepository;
import ru.nsu.crossfitbuddy.shared.security.CurrentUser;

@Service
@RequiredArgsConstructor
public class AvatarService {
    private final ProfileService profileService;
    private final UserAvatarRepository userAvatarRepository;
    private final AvatarValidator avatarValidator;

    @Transactional
    public void replaceCurrentAvatar(CurrentUser currentUser, byte[] content) {
        String contentType = avatarValidator.validate(content);
        profileService.findProfileOrThrow(currentUser.id(), true);
        var avatar = userAvatarRepository.findById(currentUser.id()).orElse(null);
        if (avatar == null) {
            userAvatarRepository.save(UserAvatar.create(currentUser.id(), contentType, content));
        } else {
            avatar.replace(contentType, content);
        }
    }

    @Transactional(readOnly = true)
    public AvatarContent getCurrentAvatar(CurrentUser currentUser) {
        profileService.findProfileOrThrow(currentUser.id(), false);
        var avatar = userAvatarRepository.findById(currentUser.id()).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "AVATAR_NOT_FOUND", "Avatar was not found."));
        return new AvatarContent(avatar.getContentType(), avatar.getContent());
    }

    @Transactional
    public void deleteCurrentAvatar(CurrentUser currentUser) {
        profileService.findProfileOrThrow(currentUser.id(), true);
        userAvatarRepository.findById(currentUser.id()).ifPresent(userAvatarRepository::delete);
    }

    public record AvatarContent(String contentType, byte[] content) {}
}
