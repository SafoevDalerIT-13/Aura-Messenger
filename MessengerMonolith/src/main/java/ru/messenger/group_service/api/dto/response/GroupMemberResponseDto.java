package ru.messenger.group_service.api.dto.response;

import lombok.Data;
import ru.messenger.group_service.domain.entity.enums.GroupMemberRole;
import ru.messenger.group_service.domain.entity.enums.GroupMemberStatus;

import java.time.Instant;

@Data
public class GroupMemberResponseDto {
    private Long id;
    private Long groupId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private GroupMemberRole role;
    private GroupMemberStatus status;
    private Boolean isAdmin;
    private Boolean canPost;
    private Boolean canInvite;
    private Boolean canManageUsers;
    private Boolean canManagePosts;
    private Instant joinedAt;
}