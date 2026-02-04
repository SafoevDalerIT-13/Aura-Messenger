package ru.messenger.group_service.api.dto.response;

import lombok.Data;
import ru.messenger.group_service.domain.entity.enums.GroupPostType;
import ru.messenger.group_service.domain.entity.enums.GroupPostStatus;

import java.time.Instant;

@Data
public class GroupPostResponseDto {
    private Long id;
    private Long groupId;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private String content;
    private GroupPostType type;
    private GroupPostStatus status;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer sharesCount;
    private Boolean likedByMe;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
}