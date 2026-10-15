package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.crossfitbuddy.core.dto.request.UserRequest;
import ru.nsu.crossfitbuddy.core.dto.response.UserResponse;
import ru.nsu.crossfitbuddy.core.entity.AppUser;
import ru.nsu.crossfitbuddy.core.exception.ServiceException;
import ru.nsu.crossfitbuddy.core.mapper.UserMapper;
import ru.nsu.crossfitbuddy.core.repository.UserRepository;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
// TODO: заглушка. Переписать логику
public class UserService {
    private static final Supplier<ServiceException> NOT_FOUND_EXCEPTION =
        () -> new ServiceException(HttpStatus.NOT_FOUND ,"Пользователь не найден");

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Пользователь с email={} уже существует", request.email());
            throw new ServiceException(HttpStatus.CONFLICT, "Пользователь с такой почтой уже существует");
        }

        AppUser saved = userRepository.save(userMapper.toEntity(request));
        log.info("Создан пользователь с id={} email={}", saved.getId(), saved.getEmail());

        return userMapper.toResponse(saved);
    }

    public UserResponse getById(long id) {
        return userRepository.findById(id)
            .map(userMapper::toResponse)
            .orElseThrow(NOT_FOUND_EXCEPTION);
    }

    @Transactional
    public UserResponse update(long id, UserRequest request) {
        AppUser appUser = userRepository.findById(id)
            .orElseThrow(NOT_FOUND_EXCEPTION);

        appUser.setEmail(request.email());
        appUser.setPasswordHash(request.passwordHash());

        AppUser saved = userRepository.save(appUser);

        log.info("Пользователь с id={} обновлен", id);

        return userMapper.toResponse(saved);
    }

    @Transactional
    // TODO: сделать soft delete
    public void delete(long id) {
        if (!userRepository.existsById(id)) {
            log.warn("Пользователь с id={} не найден для удаления", id);
            throw new ServiceException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }

        userRepository.deleteById(id);
        log.info("Пользователь с id={} удалён", id);
    }
}
