package ru.nsu.crossfitbuddy.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;

import java.util.UUID;

class UserProfileMapperTest {
    private final UserProfileMapper mapper = Mappers.getMapper(UserProfileMapper.class);

    @Test
    void mapsSeparatedProfileAndAccountToApiResponse() {
        UUID id = UUID.randomUUID();
        var account = UserAccount.builder().id(id).email("athlete@example.com").build();
        var profile = UserProfile.create(account, "Athlete", "Example");

        var response = mapper.toResponse(profile);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("athlete@example.com");
        assertThat(response.displayName()).isEqualTo("Athlete Example");
    }
}
