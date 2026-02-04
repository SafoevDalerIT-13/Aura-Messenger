package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageStatusUpdateDto {
    private Long messageId;
    private Long userId;
    private String username;
    private String status; // READ, DELIVERED
    private Instant timestamp;
}