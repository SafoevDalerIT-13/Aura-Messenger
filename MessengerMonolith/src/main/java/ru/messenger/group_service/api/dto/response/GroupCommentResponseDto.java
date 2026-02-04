package ru.messenger.group_service.api.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class GroupCommentResponseDto {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private String content;
    private Long parentCommentId;
    private Integer likesCount;
    private Integer repliesCount;
    private Boolean likedByMe;
    private Instant createdAt;
    private Instant updatedAt;
}