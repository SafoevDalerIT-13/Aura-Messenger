package ru.messenger.chat_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantDto {
    private Long id;
    private String username;
    private String login;
    private String email;
    private String phoneNumber;
    private String description;
    private String avatarUrl;
}