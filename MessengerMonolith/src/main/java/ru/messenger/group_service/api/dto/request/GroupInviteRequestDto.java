package ru.messenger.group_service.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupInviteRequestDto {

    @NotNull(message = "ID группы обязателен")
    private Long groupId;

    private Long userId;           // Для приглашения существующего пользователя
    private String email;          // Для приглашения по email
    private String message;        // Сообщение с приглашением
}