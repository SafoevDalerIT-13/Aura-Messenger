package ru.messenger.user_service.mycontact_service;

import lombok.Data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactResponseDto {
    private Long id;
    private Long userId;
    private Long friendId;
    private String friendUsername;
    private String friendAvatarUrl;
    private String nickname;
    private String status;      // PENDING, ACCEPTED, etc
    private boolean isFavorite;
    private boolean isBlocked;
    private LocalDateTime addedAt;
    private LocalDateTime lastInteractionAt;
}