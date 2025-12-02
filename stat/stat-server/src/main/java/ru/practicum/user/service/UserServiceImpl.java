package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("User with email '" + newUserRequest.getEmail() + "' already exists");
        }

        User user = User.builder()
                .email(newUserRequest.getEmail())
                .name(newUserRequest.getName())
                .build();

        User savedUser = userRepository.save(user);
        return toUserDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserByIdOrThrow(userId);

        // Временно отключаем проверку событий - их пока нет
        // if (hasUserEvents(userId)) {
        //     throw new ConflictException("Cannot delete user with events");
        // }

        userRepository.deleteById(userId);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findByIds(ids, pageable);
        }

        return users.stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    private User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}