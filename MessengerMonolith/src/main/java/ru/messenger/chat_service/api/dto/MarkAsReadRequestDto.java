package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadRequestDto {

    @NotNull(message = "ID сообщения обязательно")
    private Long messageId;

    @NotNull(message = "ID пользователя обязательно")
    private Long userId;
}