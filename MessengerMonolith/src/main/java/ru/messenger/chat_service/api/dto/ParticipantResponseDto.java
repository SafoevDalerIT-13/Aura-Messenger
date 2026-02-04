package ru.messenger.chat_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
public class ParticipantResponseDto {

    private Long userId;

    private String username;

    private Long chatId;

    private Instant joinedAt;
}