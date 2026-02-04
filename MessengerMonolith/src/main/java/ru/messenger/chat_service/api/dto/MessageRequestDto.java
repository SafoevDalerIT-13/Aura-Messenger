package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.messenger.chat_service.domain.entity.enums.MessageType;

import java.util.Set;

@Data
@Builder
public class MessageRequestDto {

    @NotNull(message = "ID чата обязателен")
    private Long chatId;

    @NotBlank(message = "Содержимое сообщения обязательно")
    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов")
    private String content;

    @NotNull(message = "Тип сообщения обязателен")
    private MessageType type;

    private Set<AttachmentRequestDto> attachments;
}