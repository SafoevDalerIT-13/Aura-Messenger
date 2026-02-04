package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TypingStatusResponseDto {
    private Long chatId;
    private Long userId;
    private String username;
    private Boolean isTyping;
    private Instant timestamp;
}