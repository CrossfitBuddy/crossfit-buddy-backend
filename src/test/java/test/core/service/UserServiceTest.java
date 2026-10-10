package test.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import test.core.dto.UserRequest;
import test.core.dto.UserResponse;
import test.core.entity.User;
import test.core.exception.ServiceException;
import test.core.mapper.UserMapper;
import test.core.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

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

    private User user;
    private UserRequest request;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("password123");

        request = new UserRequest("test@example.com", "password123");
        response = new UserResponse(1L, "test@example.com");
    }

    @Test
    @DisplayName("должен создать пользователя, если email уникален")
    void shouldCreateUserWhenEmailIsUnique() {
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(user);
        verify(userMapper).toEntity(request);
        verify(userMapper).toResponse(user);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getById(1L);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(1L);
        verify(userMapper).toResponse(user);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserRequest updateRequest = new UserRequest("new@example.com", "newpassword");

        UserResponse result = userService.update(1L, updateRequest);

        assertThat(result).isEqualTo(response);
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("newpassword");

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("должен выбросить исключение, если пользователь не найден")
    void shouldThrowExceptionWhenUserNotFoundForUpdate() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserRequest updateRequest = new UserRequest("new@example.com", "newpassword");

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
