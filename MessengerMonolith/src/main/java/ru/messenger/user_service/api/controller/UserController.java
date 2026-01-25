package ru.messenger.user_service.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import ru.messenger.user_service.api.dto.DescriptionUpdateDto;
import ru.messenger.user_service.api.dto.UserRequestDto;
import ru.messenger.user_service.api.dto.UserResponseDto;
import ru.messenger.user_service.api.dto.UsernameUpdateDto;
import ru.messenger.user_service.domain.service.UserService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Создание нового пользователя
     * POST /api/v1/users/auth
     */
    @PostMapping("/auth")
    public ResponseEntity<UserResponseDto> authUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        log.info("Вызвался метод authUser из контроллера с параметрами: username = {}, email = {}", userRequestDto.getUsername(), userRequestDto.getEmail());
        UserResponseDto createdUser = userService.authUser(userRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Получение информации о текущем пользователе
     * GET /api/v1/users/me
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        String login = userDetails.getUsername(); // ← это login, а не username!
        log.info("Запрос профиля для пользователя с логином: {}", login);

        UserResponseDto user = userService.getUserByLogin(login);
        return ResponseEntity.ok(user);
    }

    /**
     * Обновление описания профиля
     * PATCH /api/v1/users/description
     */
    @PatchMapping("/update/description")
    public ResponseEntity<UserResponseDto> updateDescription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid DescriptionUpdateDto descriptionDto) {

        String login = userDetails.getUsername();

        UserResponseDto updated = userService.updateUserDescription(login, descriptionDto.getDescription());
        return ResponseEntity.ok(updated);
    }
    /**
     * Обновление иsername профиля
     * PATCH /api/v1/users/update/username
     */
    @PatchMapping("/update/username")  // или "/username" если у тебя такой путь
    public ResponseEntity<UserResponseDto> updateUsername(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UsernameUpdateDto usernameDto) {

        String login = userDetails.getUsername(); // ← это login!
        log.info("Изменение username для login={}, новое username={}", login, usernameDto.getUsername());

        UserResponseDto updated = userService.updateUsername(login, usernameDto.getUsername());
        return ResponseEntity.ok(updated);
    }
}
