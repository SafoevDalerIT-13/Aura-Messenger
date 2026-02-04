package ru.messenger.group_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.group_service.domain.entity.GroupEntity;
import ru.messenger.group_service.domain.entity.enums.GroupType;
import ru.messenger.group_service.domain.entity.enums.GroupVisibility;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    // Основные методы
    Page<GroupEntity> findByTypeAndIsPublicTrue(GroupType type, Pageable pageable);
    Page<GroupEntity> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT g FROM GroupEntity g " +
            "JOIN g.members m " +
            "WHERE m.user.id = :userId AND m.status = 'ACTIVE'")
    Page<GroupEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND g.type = :type " +
            "AND (LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<GroupEntity> searchPublicGroups(@Param("type") GroupType type,
                                         @Param("search") String search,
                                         Pageable pageable);

    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND (LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<GroupEntity> searchAllPublicGroups(@Param("search") String search,
                                            Pageable pageable);

    // ВАЖНО: добавлен этот метод
    @Query(value = "SELECT g.*, " +
            "COALESCE(p.posts_count, 0) + COALESCE(c.comments_count, 0) as total_activity " +
            "FROM groups g " +
            "LEFT JOIN (SELECT group_id, COUNT(*) as posts_count " +
            "           FROM group_posts " +
            "           WHERE status = 'PUBLISHED' " +
            "           AND created_at >= NOW() - INTERVAL '30 days' " +
            "           GROUP BY group_id) p ON g.id = p.group_id " +
            "LEFT JOIN (SELECT p.group_id, COUNT(c.id) as comments_count " +
            "           FROM group_post_comments c " +
            "           JOIN group_posts p ON c.post_id = p.id " +
            "           WHERE p.status = 'PUBLISHED' " +
            "           AND c.created_at >= NOW() - INTERVAL '30 days' " +
            "           GROUP BY p.group_id) c ON g.id = c.group_id " +
            "WHERE g.is_public = true " +
            "ORDER BY total_activity DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<GroupEntity> findTopActiveGroups(@Param("limit") int limit);

    // Альтернативная версия метода с периодом
    @Query(value = "SELECT g.*, " +
            "COALESCE(p.posts_count, 0) + COALESCE(c.comments_count, 0) as total_activity " +
            "FROM groups g " +
            "LEFT JOIN (SELECT group_id, COUNT(*) as posts_count " +
            "           FROM group_posts " +
            "           WHERE status = 'PUBLISHED' " +
            "           AND created_at >= :since " +
            "           GROUP BY group_id) p ON g.id = p.group_id " +
            "LEFT JOIN (SELECT p.group_id, COUNT(c.id) as comments_count " +
            "           FROM group_post_comments c " +
            "           JOIN group_posts p ON c.post_id = p.id " +
            "           WHERE p.status = 'PUBLISHED' " +
            "           AND c.created_at >= :since " +
            "           GROUP BY p.group_id) c ON g.id = c.group_id " +
            "WHERE g.is_public = true " +
            "ORDER BY total_activity DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<GroupEntity> findTopActiveGroupsSince(
            @Param("limit") int limit,
            @Param("since") Instant since);

    // Топ групп по участникам
    @Query(value = "SELECT g.* FROM groups g " +
            "WHERE g.is_public = true " +
            "ORDER BY g.members_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<GroupEntity> findTopGroupsByMembers(@Param("limit") int limit);

    // Топ групп по постам
    @Query(value = "SELECT g.* FROM groups g " +
            "WHERE g.is_public = true " +
            "ORDER BY g.posts_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<GroupEntity> findTopGroupsByPosts(@Param("limit") int limit);

    // Группы с фильтрами
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE (:type IS NULL OR g.type = :type) " +
            "AND (:visibility IS NULL OR g.visibility = :visibility) " +
            "AND g.isPublic = true " +
            "AND (LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'members' THEN g.membersCount END DESC, " +
            "CASE WHEN :sortBy = 'date' THEN g.createdAt END DESC, " +
            "CASE WHEN :sortBy = 'name' THEN g.name END ASC")
    Page<GroupEntity> searchGroupsWithFilters(
            @Param("search") String search,
            @Param("type") GroupType type,
            @Param("visibility") GroupVisibility visibility,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    // Рекомендуемые группы
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND g.id NOT IN (" +
            "    SELECT gm.group.id FROM GroupMemberEntity gm " +
            "    WHERE gm.user.id = :userId AND gm.status = 'ACTIVE'" +
            ") " +
            "AND g.type IN (" +
            "    SELECT DISTINCT g2.type FROM GroupEntity g2 " +
            "    JOIN g2.members m2 " +
            "    WHERE m2.user.id = :userId AND m2.status = 'ACTIVE'" +
            ") " +
            "ORDER BY g.membersCount DESC, g.createdAt DESC")
    Page<GroupEntity> findRecommendedGroups(@Param("userId") Long userId, Pageable pageable);

    // Популярные группы
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND g.membersCount >= :minMembers " +
            "ORDER BY g.membersCount DESC, g.postsCount DESC, g.createdAt DESC")
    Page<GroupEntity> findPopularGroups(@Param("minMembers") Integer minMembers, Pageable pageable);

    // Новые группы
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND g.createdAt >= :since " +
            "ORDER BY g.createdAt DESC, g.membersCount DESC")
    Page<GroupEntity> findNewGroups(@Param("since") Instant since, Pageable pageable);

    // Активные группы
    @Query("SELECT DISTINCT g FROM GroupEntity g " +
            "JOIN GroupPostEntity p ON g.id = p.group.id " +
            "WHERE g.isPublic = true " +
            "AND p.status = 'PUBLISHED' " +
            "AND p.createdAt >= :since " +
            "ORDER BY g.membersCount DESC, g.postsCount DESC")
    Page<GroupEntity> findActiveGroups(@Param("since") Instant since, Pageable pageable);

    // Группы по диапазону участников
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND g.membersCount BETWEEN :minMembers AND :maxMembers " +
            "ORDER BY g.membersCount DESC")
    Page<GroupEntity> findByMembersCountRange(
            @Param("minMembers") Integer minMembers,
            @Param("maxMembers") Integer maxMembers,
            Pageable pageable);

    // Группы владельца
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE g.isPublic = true " +
            "AND g.owner.id = :ownerId " +
            "ORDER BY g.createdAt DESC")
    Page<GroupEntity> findPublicGroupsByOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    // Группы по типу и видимости
    List<GroupEntity> findByTypeAndVisibility(GroupType type, GroupVisibility visibility);

    // Подсчеты
    long countByOwnerId(Long ownerId);
    long countByType(GroupType type);
    long countByIsPublicTrue();
    long countByTypeAndIsPublicTrue(GroupType type);

    // Статистика по датам
    long countByCreatedAtAfter(Instant since);

    // Поиск по названию
    Optional<GroupEntity> findByName(String name);

    // Похожие группы
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "AND g.isPublic = true " +
            "ORDER BY LENGTH(g.name) ASC, g.membersCount DESC")
    List<GroupEntity> findSimilarGroupsByName(@Param("name") String name, Pageable pageable);

    // Группы по префиксу
    @Query("SELECT g FROM GroupEntity g " +
            "WHERE LOWER(g.name) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "AND g.isPublic = true " +
            "ORDER BY g.name ASC")
    Page<GroupEntity> findByPrefix(@Param("prefix") String prefix, Pageable pageable);
}