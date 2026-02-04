package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TypingStatusRequestDto {

    @NotNull(message = "ID чата обязательно")
    private Long chatId;

    @NotNull(message = "ID пользователя обязательно")
    private Long userId;

    @NotNull(message = "Статус обязателен")
    private Boolean isTyping;
}