package ru.nsu.crossfitbuddy.core.user.service;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.nsu.crossfitbuddy.common.api.ApiException;
import ru.nsu.crossfitbuddy.core.user.api.ProfileResponse;
import ru.nsu.crossfitbuddy.core.user.api.UpdateProfileRequest;
import ru.nsu.crossfitbuddy.core.user.domain.UserProfile;
import ru.nsu.crossfitbuddy.core.user.domain.UserRole;
import ru.nsu.crossfitbuddy.core.user.persistence.AthleteProfileRepository;
import ru.nsu.crossfitbuddy.core.user.persistence.TrainerProfileRepository;
import ru.nsu.crossfitbuddy.core.user.persistence.UserAvatarRepository;
import ru.nsu.crossfitbuddy.core.user.persistence.UserProfileRepository;
import ru.nsu.crossfitbuddy.shared.security.CurrentUser;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserProfileRepository userProfileRepository;
    private final AthleteProfileRepository athleteProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserAvatarRepository userAvatarRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile(CurrentUser currentUser) {
        return toResponse(findProfileOrThrow(currentUser.id(), false));
    }

    @Transactional
    public ProfileResponse updateCurrentProfile(CurrentUser currentUser, UpdateProfileRequest request) {
        var profile = findProfileOrThrow(currentUser.id(), true);
        var roles = profile.getUser().getRoles();

        if ((request.athlete() != null && !roles.contains(UserRole.ATHLETE))
            || (request.trainer() != null && !roles.contains(UserRole.TRAINER))) {
            throw new AccessDeniedException("The requested profile role is not assigned to this account");
        }

        if ((roles.contains(UserRole.ATHLETE) && request.athlete() == null)
            || (roles.contains(UserRole.TRAINER) && request.trainer() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "The profile data for every assigned role is required.");
        }

        profile.updateDetails(request.firstName(), request.lastName(), request.birthDate(), request.gender(),
            request.city(), request.about(), profile.getAvatarKey());

        if (request.athlete() != null) {
            var athleteProfile = athleteProfileRepository.findById(profile.getId())
                .orElseThrow(ProfileService::roleProfileNotFound);
            var athlete = request.athlete();

            athleteProfile.updateMeasurements(athlete.heightCm(), athlete.weightKg());
            athleteProfile.updatePreferences(athlete.fitnessLevel(), athlete.rxPreference());
        }

        if (request.trainer() != null) {
            var trainerProfile = trainerProfileRepository.findById(profile.getId())
                .orElseThrow(ProfileService::roleProfileNotFound);
            var trainer = request.trainer();

            trainerProfile.updateDetails(trainer.specializations(), trainer.experienceYears(), trainer.methodology());
        }

        return toResponse(profile);
    }

    UserProfile findProfileOrThrow(UUID id, boolean lockForUpdate) {
        return (lockForUpdate ? userProfileRepository.findForUpdate(id) : userProfileRepository.findById(id))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile was not found."));
    }

    private ProfileResponse toResponse(UserProfile profile) {
        var account = profile.getUser();
        var athlete = account.getRoles().contains(UserRole.ATHLETE)
            ? athleteProfileRepository.findById(profile.getId()).map(value -> new ProfileResponse.AthleteFields(
                value.getHeightCm(), value.getWeightKg(), value.getFitnessLevel(), value.getRxPreference()))
                .orElseThrow(ProfileService::roleProfileNotFound) : null;
        var trainer = account.getRoles().contains(UserRole.TRAINER)
            ? trainerProfileRepository.findById(profile.getId()).map(value -> new ProfileResponse.TrainerFields(
                value.getSpecializations(), value.getExperienceYears(), value.getMethodology()))
                .orElseThrow(ProfileService::roleProfileNotFound) : null;

        return new ProfileResponse(account.getId(), account.getEmail(), account.getRoles(),
            profile.getFirstName(), profile.getLastName(), profile.getDisplayName(), profile.getBirthDate(),
            profile.getGender(), profile.getCity(), profile.getAbout(),
            userAvatarRepository.existsById(profile.getId()) ? "/api/v1/users/me/avatar" : null,
            extractInitial(profile.getFirstName()) + extractInitial(profile.getLastName()), athlete, trainer);
    }

    private static String extractInitial(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.strip();
        return normalized.substring(0, normalized.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
    }

    private static ApiException roleProfileNotFound() {
        return new ApiException(HttpStatus.CONFLICT, "ROLE_PROFILE_NOT_FOUND",
            "The profile for an assigned role has not been created. Contact an administrator.");
    }
}
