package ru.messenger.user_service.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.messenger.user_service.api.dto.AvatarResponseDto;
import ru.messenger.user_service.domain.service.UserAvatarService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/avatar")
@RequiredArgsConstructor
public class UserAvatarController {

    private final UserAvatarService avatarService;

    @PostMapping
    public ResponseEntity<AvatarResponseDto> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        String login = userDetails.getUsername();
        log.info("Загрузка аватарки для пользователя с логином: {}", login);

        try {
            String avatarUrl = avatarService.uploadAvatar(login, file);

            AvatarResponseDto response = AvatarResponseDto.builder()
                    .avatarUrl(avatarUrl)
                    .message("Аватар успешно загружен")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IOException e) {
            log.error("Ошибка загрузки аватарки: {}", e.getMessage());

            AvatarResponseDto errorResponse = AvatarResponseDto.builder()
                    .message("Ошибка: " + e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping
    public ResponseEntity<AvatarResponseDto> deleteAvatar(
            @AuthenticationPrincipal UserDetails userDetails) {

        String login = userDetails.getUsername(); // ← это login, не username
        log.info("Удаление аватарки для пользователя с логином: {}", login);

        try {
            avatarService.deleteAvatar(login);

            AvatarResponseDto response = AvatarResponseDto.builder()
                    .message("Аватар успешно удален")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Ошибка удаления аватарки: {}", e.getMessage());

            AvatarResponseDto errorResponse = AvatarResponseDto.builder()
                    .message("Ошибка удаления файла")
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}