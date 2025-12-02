package ru.practicum.user.service;

import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.NewUserRequest;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);
}