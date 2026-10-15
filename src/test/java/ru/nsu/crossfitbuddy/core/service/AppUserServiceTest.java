package ru.nsu.crossfitbuddy.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.nsu.crossfitbuddy.core.dto.request.UserRequest;
import ru.nsu.crossfitbuddy.core.dto.response.UserResponse;
import ru.nsu.crossfitbuddy.core.entity.AppUser;
import ru.nsu.crossfitbuddy.core.exception.ServiceException;
import ru.nsu.crossfitbuddy.core.mapper.UserMapper;
import ru.nsu.crossfitbuddy.core.repository.UserRepository;
import ru.nsu.crossfitbuddy.utils.UserTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userRepository);
    }

    private AppUser appUser;
    private UserRequest request;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        appUser = new AppUser();
        appUser.setId(userId);
        appUser.setEmail("test@example.com");
        appUser.setPasswordHash("password123");

        request = UserTestUtils.getUserRequest();
        response = new UserResponse(userId, "test@example.com");
    }

    @Test
    @DisplayName("должен создать пользователя, если email уникален")
    void shouldCreateUserWhenEmailIsUnique() {
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(appUser);
        when(userRepository.save(appUser)).thenReturn(appUser);
        when(userMapper.toResponse(appUser)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(appUser);
        verify(userMapper).toEntity(request);
        verify(userMapper).toResponse(appUser);
    }
    @Test
    @DisplayName("должен выбросить исключение, если email уже существует")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> userService.create(request));

        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("должен вернуть пользователя по id")
    void shouldReturnUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(userMapper.toResponse(appUser)).thenReturn(response);

        UserResponse result = userService.getById(1L);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(1L);
        verify(userMapper).toResponse(appUser);
    }

    @Test
    @DisplayName("должен выбросить исключение, если пользователь не найден")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> userService.getById(999L));

        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(userRepository).findById(999L);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("должен обновить пользователя")
    void shouldUpdateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(userRepository.save(any(AppUser.class))).thenReturn(appUser);
        when(userMapper.toResponse(appUser)).thenReturn(response);

        UserRequest updateRequest = UserTestUtils.getUpdatedUserRequest();

        UserResponse result = userService.update(1L, updateRequest);

        assertThat(result).isEqualTo(response);
        assertThat(appUser.getEmail()).isEqualTo("new@example.com");
        assertThat(appUser.getPasswordHash()).isEqualTo("newpassword");

        verify(userRepository).findById(1L);
        verify(userRepository).save(appUser);
        verify(userMapper).toResponse(appUser);
    }

    @Test
    @DisplayName("должен выбросить исключение, если пользователь не найден")
    void shouldThrowExceptionWhenUserNotFoundForUpdate() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserRequest updateRequest = UserTestUtils.getUpdatedUserRequest();

        ServiceException exception = assertThrows(ServiceException.class,
            () -> userService.update(999L, updateRequest));

        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("должен удалить пользователя")
    void shouldDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("должен выбросить исключение, если пользователь не найден")
    void shouldThrowExceptionWhenUserNotFoundForDelete() {
        when(userRepository.existsById(999L)).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> userService.delete(999L));

        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
