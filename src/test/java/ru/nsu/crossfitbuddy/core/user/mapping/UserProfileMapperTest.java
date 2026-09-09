package ru.nsu.crossfitbuddy.core.user.mapping;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.nsu.crossfitbuddy.core.user.domain.UserProfile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileMapperTest {
    private final UserProfileMapper mapper = Mappers.getMapper(UserProfileMapper.class);

    @Test
    void mapsLombokModelToApiResponse() {
        UUID id = UUID.randomUUID();
        var profile = UserProfile.builder()
                .id(id)
                .email("athlete@example.com")
                .displayName("Athlete")
                .build();

        var response = mapper.toResponse(profile);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("athlete@example.com");
        assertThat(response.displayName()).isEqualTo("Athlete");
    }
}
