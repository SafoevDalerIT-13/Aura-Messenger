package ru.messenger.user_service.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.user_service.api.dto.UserRequestDto;
import ru.messenger.user_service.api.dto.UserResponseDto;
import ru.messenger.user_service.api.mapper.UserMapper;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;
import ru.messenger.user_service.domain.service.exception.UserAlreadyExistsException;
import ru.messenger.user_service.domain.service.exception.UserNotFoundException;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;


    // Метод для авторизации пользователя и занесение в основную бд
    public UserResponseDto authUser(UserRequestDto user) {
        log.info("Вызвался метод createUser из UserService!");
        if(user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("Пользователь с таким Email уже существует!");
        }
        if(user.getUsername() != null && userRepository.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExistsException("Пользователь с таким Username уже существует!");
        }

        UserEntity userEntity = userMapper.toEntity(user);
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));
        UserEntity savedEntity = userRepository.save(userEntity);

        return userMapper.toResponseDto(savedEntity);
    }

    // Получение Пользователя по email
    public UserResponseDto getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с email " + email + " не найден"));
        return userMapper.toResponseDto(user);
    }

    // Получение Пользователя по username
    public UserResponseDto getUserByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь " + username + " не найден"));
        return userMapper.toResponseDto(user);
    }

    // Обновление Описания профиля у пользователя
    public UserResponseDto updateUserDescription(String username, String description) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        user.setDescription(description);
        UserEntity updated = userRepository.save(user);

        return userMapper.toResponseDto(updated);
    }

    // Обновление username профиля у пользователя
    @Transactional
    public UserResponseDto updateUserUsername(String currentUsername, String newUsername) {
        UserEntity user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (newUsername.equals(currentUsername)) {
            return userMapper.toResponseDto(user);
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Имя пользователя '" + newUsername + "' уже занято");
        }


        user.setUsername(newUsername);
        UserEntity updated = userRepository.save(user);

        return userMapper.toResponseDto(updated);
    }

    // Обновление у пользователя описания и username
    public UserResponseDto updateUserProfile(Long userId, UserRequestDto updateDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        if (updateDto.getDescription() != null) {
            user.setDescription(updateDto.getDescription());
        }

        if (updateDto.getUsername() != null &&
                !updateDto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(updateDto.getUsername())) {
                throw new UserAlreadyExistsException("Имя пользователя занято");
            }
            user.setUsername(updateDto.getUsername());
        }

        UserEntity updated = userRepository.save(user);
        return userMapper.toResponseDto(updated);
    }

}
