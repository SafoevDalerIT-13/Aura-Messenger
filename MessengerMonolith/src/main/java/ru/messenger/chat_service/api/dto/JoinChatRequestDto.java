package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinChatRequestDto {
    private Long chatId;
}