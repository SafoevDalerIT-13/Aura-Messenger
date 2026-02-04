package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.messenger.chat_service.domain.entity.enums.MessageStatus;

@Data
@Builder
public class MessageUpdateRequestDto {

    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов")
    private String content;

    @NotNull(message = "Статус сообщения обязателен")
    private MessageStatus status;
}