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
        String username = userDetails.getUsername();
        UserResponseDto user = userService.getUserByUsername(username);
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

        String username = userDetails.getUsername();
        UserResponseDto updated = userService.updateUserDescription(username, descriptionDto.getDescription());
        return ResponseEntity.ok(updated);
    }
    /**
     * Обновление иsername профиля
     * PATCH /api/v1/users/update/username
     */
    @PatchMapping("/update/username")
    public ResponseEntity<Map<String, String>> updateUsername(
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication,
            @RequestBody @Valid UsernameUpdateDto usernameDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        String currentUsername = userDetails.getUsername();

        try {
            UserResponseDto updatedUser = userService.updateUserUsername(
                    currentUsername,
                    usernameDto.getUsername()
            );

            new SecurityContextLogoutHandler().logout(request, response, authentication);

            return ResponseEntity.ok(Map.of(
                    "message", "Имя пользователя изменено на: " + updatedUser.getUsername(),
                    "newUsername", updatedUser.getUsername(),
                    "action", "logout"
            ));

        } catch (Exception e) {
            log.error("Ошибка при изменении username: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
