package ru.messenger.user_service.mycontact_service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponseDto {
    private Long id;
    private String username;
    private String login;
    private String email;
    private String avatarUrl;
    private boolean isOnline;
    private String friendshipStatus; // "NONE", "PENDING_OUTGOING", "PENDING_INCOMING", "FRIENDS", "BLOCKED"
    private Long contactId; // ID записи в user_contacts (если есть)
}