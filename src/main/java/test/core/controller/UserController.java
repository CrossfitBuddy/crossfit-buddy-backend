package test.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import test.core.dto.UserRequest;
import test.core.dto.UserResponse;
import test.core.service.UserService;

@RestController
@RequestMapping(Paths.USER)
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping
    public UserResponse create(@RequestBody UserRequest request) {
        return userService.create(request);
    }

    @GetMapping(Paths.USER_BY_ID)
    public UserResponse getById(@PathVariable long id) {
        return userService.getById(id);
    }

    @PutMapping(Paths.USER_BY_ID)
    public UserResponse update(@PathVariable long id, @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping(Paths.USER_BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        userService.delete(id);
    }
}
