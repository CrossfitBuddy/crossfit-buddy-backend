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
        if ((request.hasField("athlete") && !roles.contains(UserRole.ATHLETE))
            || (request.hasField("trainer") && !roles.contains(UserRole.TRAINER))) {
            throw new AccessDeniedException("The requested profile role is not assigned to this account");
        }
        profile.updateDetails(
            request.hasField("firstName") ? request.getFirstName() : profile.getFirstName(),
            request.hasField("lastName") ? request.getLastName() : profile.getLastName(),
            request.hasField("birthDate") ? request.getBirthDate() : profile.getBirthDate(),
            request.hasField("gender") ? request.getGender() : profile.getGender(),
            request.hasField("city") ? request.getCity() : profile.getCity(),
            request.hasField("about") ? request.getAbout() : profile.getAbout(), profile.getAvatarKey());
        if (request.hasField("athlete")) {
            var athleteProfile = athleteProfileRepository.findById(profile.getId())
                .orElseThrow(ProfileService::roleProfileNotFound);
            var athletePatch = request.getAthlete();
            athleteProfile.updateMeasurements(
                athletePatch.hasField("heightCm") ? athletePatch.getHeightCm() : athleteProfile.getHeightCm(),
                athletePatch.hasField("weightKg") ? athletePatch.getWeightKg() : athleteProfile.getWeightKg());
            athleteProfile.updatePreferences(
                athletePatch.hasField("fitnessLevel")
                    ? athletePatch.getFitnessLevel() : athleteProfile.getFitnessLevel(),
                athletePatch.hasField("rxPreference")
                    ? athletePatch.getRxPreference() : athleteProfile.getRxPreference());
        }
        if (request.hasField("trainer")) {
            var trainerProfile = trainerProfileRepository.findById(profile.getId())
                .orElseThrow(ProfileService::roleProfileNotFound);
            var trainerPatch = request.getTrainer();
            trainerProfile.updateDetails(
                trainerPatch.hasField("specializations")
                    ? trainerPatch.getSpecializations() : trainerProfile.getSpecializations(),
                trainerPatch.hasField("experienceYears")
                    ? trainerPatch.getExperienceYears() : trainerProfile.getExperienceYears(),
                trainerPatch.hasField("methodology") ? trainerPatch.getMethodology() : trainerProfile.getMethodology());
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
            userAvatarRepository.existsById(profile.getId()) ? "/api/users/me/avatar" : null,
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
