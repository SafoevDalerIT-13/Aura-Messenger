package ru.messenger.group_service.api.dto.response;

import lombok.Data;
import ru.messenger.group_service.domain.entity.enums.GroupInviteStatus;
import java.time.Instant;

@Data
public class GroupInviteResponseDto {
    private Long id;
    private Long groupId;
    private String groupName;
    private Long inviterId;
    private String inviterUsername;
    private Long invitedId;
    private String invitedUsername;
    private String invitedEmail;
    private GroupInviteStatus status;
    private String message;
    private Instant expiresAt;
    private Instant createdAt;
    private Boolean isExpired;
    private Boolean isValid;
}