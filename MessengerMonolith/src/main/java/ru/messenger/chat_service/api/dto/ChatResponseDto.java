package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;
import ru.messenger.chat_service.domain.entity.enums.ChatType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class ChatResponseDto {

    private Long id;

    private String name;

    private ChatType type;

    private String avatarUrl;

    private Long lastMessageId;

    private Set<Long> participantIds;

    private Instant createdAt;

    private Instant updatedAt;

    // Полная информация об участниках
    private List<ChatParticipantDto> participants;
}