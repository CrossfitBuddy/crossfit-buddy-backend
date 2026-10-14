package ru.nsu.crossfitbuddy.core.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProfileTest {
    @Test
    void normalizesPersonalDataAndBuildsDisplayNameFromNames() {
        var user = UserAccount.builder().build();
        var profile = UserProfile.create(user, "  Иван ", " Иванов  ");
        assertThat(profile.getDisplayName()).isEqualTo("Иван Иванов");
        profile.updateDetails("Иван", "Иванов", null, null, "  Новосибирск ", " ", " avatars/1.webp ");
        assertThat(profile.getCity()).isEqualTo("Новосибирск");
        assertThat(profile.getAbout()).isNull();
        assertThat(profile.getAvatarKey()).isEqualTo("avatars/1.webp");
    }

    @Test
    void requiresAccountForEveryProfile() {
        assertThatThrownBy(() -> UserProfile.create(null, "Иван", "Иванов")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AthleteProfile.create(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TrainerProfile.create(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesNotExposeMutableRoleCollection() {
        var account = UserAccount.builder().build();
        assertThatThrownBy(() -> account.getRoles().add(UserRole.ADMIN))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
