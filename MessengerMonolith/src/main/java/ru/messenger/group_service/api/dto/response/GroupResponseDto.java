package ru.messenger.group_service.api.dto.response;

import lombok.Data;
import ru.messenger.group_service.domain.entity.enums.GroupType;
import ru.messenger.group_service.domain.entity.enums.GroupVisibility;

import java.time.Instant;

@Data
public class GroupResponseDto {
    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private String coverUrl;
    private GroupType type;
    private GroupVisibility visibility;
    private Long ownerId;
    private String ownerUsername;
    private Boolean isPublic;
    private Integer membersCount;
    private Integer postsCount;
    private Instant createdAt;
    private Instant updatedAt;
}