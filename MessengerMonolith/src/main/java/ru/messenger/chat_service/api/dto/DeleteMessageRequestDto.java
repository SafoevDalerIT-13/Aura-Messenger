package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteMessageRequestDto {
    private Long chatId;
    private Long messageId;
}