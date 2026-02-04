package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.messenger.chat_service.domain.entity.enums.MessageStatus;
import ru.messenger.chat_service.domain.entity.enums.MessageType;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
public class MessageResponseDto {

    private Long id;

    private Long chatId;

    private Long senderId;

    private String senderUsername;

    private String content;

    private MessageType type;

    private MessageStatus status;

    private Set<Long> readBy;

    private Set<AttachmentResponseDto> attachments;

    private Instant sentAt;
}