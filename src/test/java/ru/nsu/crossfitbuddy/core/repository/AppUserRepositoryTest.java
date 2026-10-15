package ru.nsu.crossfitbuddy.core.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.nsu.crossfitbuddy.core.entity.AppUser;
import ru.nsu.crossfitbuddy.utils.UserTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AppUserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnTrueWhenEmailExists() {
        AppUser appUser = UserTestUtils.getAppUser();
        userRepository.save(appUser);

        assertThat(userRepository.existsByEmail(appUser.getEmail())).isTrue();
    }
}
