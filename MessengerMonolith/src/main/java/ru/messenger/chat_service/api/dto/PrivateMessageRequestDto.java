package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;
import ru.messenger.chat_service.domain.entity.enums.MessageType;

@Data
@Builder
public class PrivateMessageRequestDto {
    private Long recipientId;
    private String content;
    private MessageType type;
}
