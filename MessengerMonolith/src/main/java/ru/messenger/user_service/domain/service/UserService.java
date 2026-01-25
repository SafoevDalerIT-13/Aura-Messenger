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
import ru.messenger.user_service.domain.service.exception.InvalidRegistrationException;
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

        if (!user.isValid()) {
            throw new InvalidRegistrationException("Укажите email ИЛИ номер телефона");
        }

        if(user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("Пользователь с таким Email уже существует!");
        }
        if(user.getLogin() != null && userRepository.existsByLogin(user.getLogin())) {
            throw new UserAlreadyExistsException("Пользователь с таким логином уже существует!");
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
                throw new UserAlreadyExistsException("Этот номер телефона уже используется");
            }
        }

        UserEntity userEntity = userMapper.toEntity(user);
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));
        userEntity.setAvatarUrl("/uploads/default-avatar.png");


        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            userEntity.setEmail(user.getEmail().trim());
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            userEntity.setPhoneNumber(cleanPhoneNumber(user.getPhoneNumber()));
        }




        UserEntity savedEntity = userRepository.save(userEntity);

        return userMapper.toResponseDto(savedEntity);
    }

    private String cleanPhoneNumber(String phone) {
        return phone.replaceAll("[^\\d+]", "");
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

    // Получение Пользователя по username
    public UserResponseDto getUserByLogin(String login) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException("Пользователь " + login + " не найден"));
        return userMapper.toResponseDto(user);
    }

    // Обновление Описания профиля у пользователя
    @Transactional
    public UserResponseDto updateUserDescription(String login, String description) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с логином '" + login + "' не найден"));

        user.setDescription(description);
        UserEntity updated = userRepository.save(user);
        return userMapper.toResponseDto(updated);
    }

    // Обновление username профиля у пользователя
    @Transactional
    public UserResponseDto updateUsername(String login, String newUsername) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Пользователь с логином '" + login + "' не найден"));

        log.info("Найден пользователь: id={}, текущий username={}, новый username={}",
                user.getId(), user.getUsername(), newUsername);

        if (newUsername.equals(user.getUsername())) {
            log.info("Username не изменился");
            return userMapper.toResponseDto(user);
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Имя пользователя '" + newUsername + "' уже занято");
        }
        user.setUsername(newUsername);
        UserEntity updated = userRepository.save(user);

        log.info("Username успешно изменен: {} -> {}", login, newUsername);
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
