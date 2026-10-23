package ru.nsu.crossfitbuddy.core.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ru.nsu.crossfitbuddy.core.dto.request.UpdateProfileRequest;
import ru.nsu.crossfitbuddy.core.entity.*;
import ru.nsu.crossfitbuddy.core.repository.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class ProfileServiceTest {
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final AthleteProfileRepository athleteProfileRepository = mock(AthleteProfileRepository.class);
    private final TrainerProfileRepository trainerProfileRepository = mock(TrainerProfileRepository.class);
    private final UserAvatarRepository userAvatarRepository = mock(UserAvatarRepository.class);
    private final ProfileService profileService = new ProfileService(userProfileRepository,
        athleteProfileRepository, trainerProfileRepository, userAvatarRepository);
    private final UUID id = UUID.randomUUID();
    private final UserAccount account = UserAccount.builder().id(id).email("test@example.com")
        .roles(Set.of(UserRole.ATHLETE)).build();
    private final UserProfile profile = UserProfile.create(account, "Иван", "Иванов");
    private final CurrentUser currentUser = new CurrentUser(id, account.getEmail(), "ATHLETE");
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("PUT очищает пропущенные необязательные поля")
    void replacesOptionalFields() throws Exception {
        profile.updateDetails("Иван", "Иванов", null, null, "Москва", "Описание", null);
        when(userProfileRepository.findForUpdate(id)).thenReturn(Optional.of(profile));
        when(athleteProfileRepository.findById(id)).thenReturn(Optional.of(AthleteProfile.create(account)));
        var result = profileService.updateCurrentProfile(currentUser,
            objectMapper.readValue("{\"firstName\":\"Иван\",\"lastName\":\"Иванов\",\"athlete\":{},\"city\":null}",
                UpdateProfileRequest.class));
        assertThat(result.city()).isNull();
        assertThat(result.about()).isNull();
        assertThat(result.firstName()).isEqualTo("Иван");
        assertThat(result.initials()).isEqualTo("ИИ");
    }

    @Test
    @DisplayName("Без нужной роли профиль не меняется")
    void rejectsRoleBeforeMutatingAnything() throws Exception {
        when(userProfileRepository.findForUpdate(id)).thenReturn(Optional.of(profile));
        var request = objectMapper.readValue("{\"firstName\":\"Пётр\",\"trainer\":{\"experienceYears\":5}}",
            UpdateProfileRequest.class);
        assertThatThrownBy(() -> profileService.updateCurrentProfile(currentUser,
            request)).isInstanceOf(AccessDeniedException.class);
        assertThat(profile.getFirstName()).isEqualTo("Иван");
        verifyNoInteractions(trainerProfileRepository, athleteProfileRepository, userAvatarRepository);
    }

    @Test
    @DisplayName("PUT очищает пропущенные поля атлета")
    void replacesNestedFields() throws Exception {
        var athlete = AthleteProfile.create(account);
        athlete.updateMeasurements(170, new java.math.BigDecimal("75.50"));
        athlete.updatePreferences(FitnessLevel.ADVANCED, RxPreference.RX);
        when(userProfileRepository.findForUpdate(id)).thenReturn(Optional.of(profile));
        when(athleteProfileRepository.findById(id)).thenReturn(Optional.of(athlete));
        var result = profileService.updateCurrentProfile(currentUser,
            objectMapper.readValue("{\"firstName\":\"Иван\",\"lastName\":\"Иванов\",\"athlete\":{\"heightCm\":180}}",
            UpdateProfileRequest.class));
        assertThat(result.athlete().heightCm()).isEqualTo(180);
        assertThat(result.athlete().weightKg()).isNull();
        assertThat(result.athlete().rxPreference()).isNull();
    }
}
