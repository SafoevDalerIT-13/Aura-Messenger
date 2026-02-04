package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebSocketMessageDto {

    @NotBlank(message = "Тип сообщения обязателен")
    private String type; // "MESSAGE", "TYPING", "READ_RECEIPT", "USER_STATUS"

    @NotNull(message = "Полезная нагрузка обязательна")
    private Object payload;

    private Long timestamp;

    @NotNull(message = "ID отправителя обязательно")
    private Long senderId;
}