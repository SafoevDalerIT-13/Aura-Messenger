package ru.messenger.group_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.group_service.domain.entity.GroupMemberEntity;
import ru.messenger.group_service.domain.entity.enums.GroupMemberStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {

    // Основные методы
    Optional<GroupMemberEntity> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMemberEntity> findByGroupId(Long groupId);
    Page<GroupMemberEntity> findByGroupId(Long groupId, Pageable pageable);

    // Проверки существования
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupMemberStatus status);

    // Подсчеты
    long countByGroupId(Long groupId);
    long countByGroupIdAndStatus(Long groupId, GroupMemberStatus status);

    // Поиск по статусу
    @Query("SELECT gm FROM GroupMemberEntity gm " +
            "WHERE gm.group.id = :groupId " +
            "AND gm.status = :status")
    Page<GroupMemberEntity> findByGroupIdAndStatus(@Param("groupId") Long groupId,
                                                   @Param("status") GroupMemberStatus status,
                                                   Pageable pageable);

    // Активные участники за период
    @Query("SELECT COUNT(DISTINCT gm.user.id) FROM GroupMemberEntity gm " +
            "WHERE gm.group.id = :groupId " +
            "AND gm.status = 'ACTIVE' " +
            "AND EXISTS (SELECT 1 FROM GroupPostEntity p WHERE p.group.id = :groupId " +
            "            AND p.author.id = gm.user.id AND p.createdAt BETWEEN :startDate AND :endDate) " +
            "OR EXISTS (SELECT 1 FROM GroupPostCommentEntity c JOIN c.post p " +
            "           WHERE p.group.id = :groupId AND c.author.id = gm.user.id " +
            "           AND c.createdAt BETWEEN :startDate AND :endDate)")
    long countActiveMembersInPeriod(@Param("groupId") Long groupId,
                                    @Param("startDate") Instant startDate,
                                    @Param("endDate") Instant endDate);

    // Топ активных пользователей в группе
    @Query(value = "SELECT u.id, u.username, " +
            "COALESCE(p.posts_count, 0) as posts_count, " +
            "COALESCE(c.comments_count, 0) as comments_count " +
            "FROM users u " +
            "JOIN group_members gm ON u.id = gm.user_id " +
            "LEFT JOIN (SELECT author_id, COUNT(*) as posts_count " +
            "           FROM group_posts WHERE group_id = :groupId " +
            "           GROUP BY author_id) p ON u.id = p.author_id " +
            "LEFT JOIN (SELECT c.author_id, COUNT(*) as comments_count " +
            "           FROM group_post_comments c " +
            "           JOIN group_posts p ON c.post_id = p.id " +
            "           WHERE p.group_id = :groupId " +
            "           GROUP BY c.author_id) c ON u.id = c.author_id " +
            "WHERE gm.group_id = :groupId AND gm.status = 'ACTIVE' " +
            "ORDER BY (COALESCE(p.posts_count, 0) + COALESCE(c.comments_count, 0)) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopActiveUsersInGroup(@Param("groupId") Long groupId,
                                             @Param("limit") int limit);

    // Поиск по пользователю с определенной ролью
    @Query("SELECT gm FROM GroupMemberEntity gm " +
            "WHERE gm.user.id = :userId " +
            "AND gm.role = :role " +
            "AND gm.status = 'ACTIVE'")
    List<GroupMemberEntity> findByUserIdAndRole(@Param("userId") Long userId,
                                                @Param("role") ru.messenger.group_service.domain.entity.enums.GroupMemberRole role);

    // Проверка, является ли пользователь администратором группы
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
            "FROM GroupMemberEntity gm " +
            "WHERE gm.group.id = :groupId " +
            "AND gm.user.id = :userId " +
            "AND (gm.isAdmin = true OR gm.role = 'OWNER') " +
            "AND gm.status = 'ACTIVE'")
    boolean isUserAdmin(@Param("groupId") Long groupId,
                        @Param("userId") Long userId);
}