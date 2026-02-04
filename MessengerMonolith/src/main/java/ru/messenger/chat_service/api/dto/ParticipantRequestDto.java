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
public class ParticipantRequestDto {

    @NotNull(message = "ID пользователя обязательно")
    private Long userId;

    @NotNull(message = "ID чата обязательно")
    private Long chatId;
}