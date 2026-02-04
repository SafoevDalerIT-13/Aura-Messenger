package ru.messenger.group_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.messenger.group_service.domain.entity.enums.GroupType;
import ru.messenger.group_service.domain.entity.enums.GroupVisibility;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupVisibility visibility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "members_count", nullable = false)
    @Builder.Default
    private Integer membersCount = 1;

    @Column(name = "posts_count", nullable = false)
    @Builder.Default
    private Integer postsCount = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMemberEntity> members = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupPostEntity> posts = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupInviteEntity> invites = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isOwner(Long userId) {
        return owner.getId().equals(userId);
    }

    public boolean isAdmin(Long userId) {
        return members.stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .anyMatch(GroupMemberEntity::getIsAdmin);
    }

    public boolean isMember(Long userId) {
        return members.stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
    }
}