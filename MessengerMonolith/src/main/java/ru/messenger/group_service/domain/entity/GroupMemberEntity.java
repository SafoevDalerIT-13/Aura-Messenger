package ru.messenger.group_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.messenger.group_service.domain.entity.enums.GroupMemberRole;
import ru.messenger.group_service.domain.entity.enums.GroupMemberStatus;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.time.Instant;

@Entity
@Table(name = "group_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GroupMemberRole role = GroupMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GroupMemberStatus status = GroupMemberStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    @CreationTimestamp
    private Instant joinedAt;

    @Column(name = "is_admin")
    @Builder.Default
    private Boolean isAdmin = false;

    @Column(name = "can_post")
    @Builder.Default
    private Boolean canPost = true;

    @Column(name = "can_invite")
    @Builder.Default
    private Boolean canInvite = false;

    @Column(name = "can_manage_users")
    @Builder.Default
    private Boolean canManageUsers = false;

    @Column(name = "can_manage_posts")
    @Builder.Default
    private Boolean canManagePosts = false;

    public boolean isActive() {
        return status == GroupMemberStatus.ACTIVE;
    }

    public boolean isBanned() {
        return status == GroupMemberStatus.BANNED;
    }
}