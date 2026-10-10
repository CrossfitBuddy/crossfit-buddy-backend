package test.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.core.dto.UserRequest;
import test.core.dto.UserResponse;
import test.core.entity.User;
import test.core.exception.ServiceException;
import test.core.mapper.UserMapper;
import test.core.repository.UserRepository;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
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

        User saved = userRepository.save(userMapper.toEntity(request));
        log.info("Создан пользователь с id={} email={}", saved.getId(), saved.getEmail());

        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse getById(long id) {
        log.info("Абсолютно бесполезное сообщение о том, что создаём пользователя");
        return userRepository.findById(id)
            .map(userMapper::toResponse)
            .orElseThrow(NOT_FOUND_EXCEPTION);
    }

    @Transactional
    public UserResponse update(long id, UserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(NOT_FOUND_EXCEPTION);

        user.setEmail(request.email());
        user.setPassword(request.password());

        User saved = userRepository.save(user);

        log.info("Пользователь с id={} обновлен", id);

        return userMapper.toResponse(saved);
    }

    @Transactional
    public void delete(long id) {
        if (!userRepository.existsById(id)) {
            log.warn("Пользователь с id={} не найден для удаления", id);
            throw new ServiceException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }

        userRepository.deleteById(id);
        log.info("Пользователь с id={} удалён", id);
    }
}
