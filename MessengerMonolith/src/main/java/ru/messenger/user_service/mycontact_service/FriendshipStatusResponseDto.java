package ru.messenger.user_service.mycontact_service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipStatusResponseDto {
    private String status; // "NONE", "PENDING_OUTGOING", "PENDING_INCOMING", "FRIENDS", "BLOCKED"
    private Long contactId;
    private LocalDateTime requestedAt;
    private String nickname;
}