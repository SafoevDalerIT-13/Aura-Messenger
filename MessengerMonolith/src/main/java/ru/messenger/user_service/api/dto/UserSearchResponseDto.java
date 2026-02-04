package ru.messenger.user_service.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserSearchResponseDto {
    private Long id;
    private String login;
    private String username;
    private String avatarUrl;
    private String description;
    private boolean isOnline;
    private Instant lastSeenAt;
    private String lastSeenFormatted;
    private boolean isContact;
}