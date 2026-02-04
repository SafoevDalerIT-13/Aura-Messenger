package ru.messenger.group_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.group_service.domain.entity.GroupPostEntity;
import ru.messenger.group_service.domain.entity.enums.GroupPostStatus;
import ru.messenger.group_service.domain.entity.enums.GroupPostType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupPostRepository extends JpaRepository<GroupPostEntity, Long> {

    // Основные методы поиска
    Page<GroupPostEntity> findByGroupId(Long groupId, Pageable pageable);
    Page<GroupPostEntity> findByGroupIdAndStatus(Long groupId, GroupPostStatus status, Pageable pageable);
    Page<GroupPostEntity> findByAuthorId(Long authorId, Pageable pageable);

    // Поиск по типу
    Page<GroupPostEntity> findByGroupIdAndType(Long groupId, GroupPostType type, Pageable pageable);
    Page<GroupPostEntity> findByGroupIdAndTypeAndStatus(Long groupId, GroupPostType type,
                                                        GroupPostStatus status, Pageable pageable);

    // Поиск по периоду времени
    Page<GroupPostEntity> findByGroupIdAndCreatedAtBetween(
            @Param("groupId") Long groupId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    Page<GroupPostEntity> findByGroupIdAndStatusAndCreatedAtBetween(
            @Param("groupId") Long groupId,
            @Param("status") GroupPostStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    Page<GroupPostEntity> findByAuthorIdAndCreatedAtBetween(
            @Param("authorId") Long authorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    // Лента пользователя
    @Query("SELECT p FROM GroupPostEntity p " +
            "JOIN GroupMemberEntity m ON p.group.id = m.group.id " +
            "WHERE m.user.id = :userId " +
            "AND m.status = 'ACTIVE' " +
            "AND p.status = 'PUBLISHED' " +
            "ORDER BY p.createdAt DESC")
    Page<GroupPostEntity> findFeedForUser(@Param("userId") Long userId, Pageable pageable);

    // Популярные посты (по лайкам)
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "ORDER BY p.likesCount DESC, p.createdAt DESC")
    Page<GroupPostEntity> findPopularPostsByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    // Черновики пользователя в группе
    Page<GroupPostEntity> findByGroupIdAndAuthorIdAndStatus(
            Long groupId, Long authorId, GroupPostStatus status, Pageable pageable);

    // Подсчеты
    long countByGroupId(Long groupId);
    long countByGroupIdAndStatus(Long groupId, GroupPostStatus status);
    long countByAuthorId(Long authorId);
    long countByGroupIdAndAuthorId(Long groupId, Long authorId);
    long countByGroupIdAndAuthorIdAndStatus(Long groupId, Long authorId, GroupPostStatus status);

    // Статистика по датам - ВАЖНО: добавлен этот метод
    long countByGroupIdAndCreatedAtBetween(Long groupId, Instant startDate, Instant endDate);

    long countByGroupIdAndAuthorIdAndCreatedAtBetween(Long groupId, Long authorId,
                                                      Instant startDate, Instant endDate);

    // Метод для подсчета постов после определенной даты - ВАЖНО: добавлен этот метод
    long countByGroupIdAndAuthorIdAndCreatedAtAfter(
            @Param("groupId") Long groupId,
            @Param("authorId") Long authorId,
            @Param("since") Instant since);

    // Аналогичные методы для других комбинаций
    long countByGroupIdAndCreatedAtAfter(Long groupId, Instant since);
    long countByAuthorIdAndCreatedAtAfter(Long authorId, Instant since);
    long countByGroupIdAndStatusAndCreatedAtAfter(Long groupId, GroupPostStatus status, Instant since);
    long countByGroupIdAndAuthorIdAndStatusAndCreatedAtAfter(
            Long groupId, Long authorId, GroupPostStatus status, Instant since);

    // Методы для статистики
    @Query("SELECT COUNT(p) FROM GroupPostEntity p WHERE p.createdAt > :since")
    long countByCreatedAtAfter(@Param("since") Instant since);

    @Query(value = "SELECT DATE(p.created_at) as date, COUNT(p.id) as count " +
            "FROM group_posts p " +
            "WHERE p.group_id = :groupId " +
            "AND p.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(p.created_at) " +
            "ORDER BY DATE(p.created_at)", nativeQuery = true)
    List<Object[]> countPostsByDay(@Param("groupId") Long groupId,
                                   @Param("startDate") Instant startDate,
                                   @Param("endDate") Instant endDate);

    // Поиск поста с проверкой группы
    Optional<GroupPostEntity> findByIdAndGroupId(Long id, Long groupId);

    // Поиск по ключевым словам в группе
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "AND (LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.createdAt DESC")
    Page<GroupPostEntity> searchInGroup(@Param("groupId") Long groupId,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    // Поиск постов по нескольким группам
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id IN :groupIds " +
            "AND p.status = 'PUBLISHED' " +
            "ORDER BY p.createdAt DESC")
    Page<GroupPostEntity> findByGroupIds(@Param("groupIds") List<Long> groupIds, Pageable pageable);

    // Посты с прикрепленными файлами
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "AND EXISTS (SELECT 1 FROM AttachmentEntity a WHERE a.message.id = p.id) " +
            "ORDER BY p.createdAt DESC")
    Page<GroupPostEntity> findPostsWithAttachments(@Param("groupId") Long groupId, Pageable pageable);

    // Посты, упорядоченные по последнему комментарию
    @Query("SELECT p FROM GroupPostEntity p " +
            "LEFT JOIN GroupPostCommentEntity c ON p.id = c.post.id " +
            "WHERE p.group.id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "GROUP BY p.id " +
            "ORDER BY MAX(c.createdAt) DESC NULLS LAST, p.createdAt DESC")
    Page<GroupPostEntity> findPostsOrderedByLastComment(@Param("groupId") Long groupId, Pageable pageable);

    // Посты с определенным количеством лайков
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "AND p.likesCount >= :minLikes " +
            "ORDER BY p.likesCount DESC, p.createdAt DESC")
    Page<GroupPostEntity> findPostsWithMinLikes(@Param("groupId") Long groupId,
                                                @Param("minLikes") Integer minLikes,
                                                Pageable pageable);

    // Анонсы (важные посты)
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.type = 'ANNOUNCEMENT' " +
            "AND p.status = 'PUBLISHED' " +
            "ORDER BY p.createdAt DESC")
    Page<GroupPostEntity> findAnnouncements(@Param("groupId") Long groupId, Pageable pageable);

    // События (с датой публикации в будущем)
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.type = 'EVENT' " +
            "AND p.status = 'PUBLISHED' " +
            "AND p.publishedAt > CURRENT_TIMESTAMP " +
            "ORDER BY p.publishedAt ASC")
    Page<GroupPostEntity> findUpcomingEvents(@Param("groupId") Long groupId, Pageable pageable);

    // Посты с наибольшим вовлечением (лайки + комментарии)
    @Query("SELECT p FROM GroupPostEntity p " +
            "WHERE p.group.id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "ORDER BY (p.likesCount + p.commentsCount) DESC, p.createdAt DESC")
    Page<GroupPostEntity> findMostEngagingPosts(@Param("groupId") Long groupId, Pageable pageable);

    // Топ авторов за период в группе
    @Query(value = "SELECT p.author_id, u.username, COUNT(p.id) as posts_count " +
            "FROM group_posts p " +
            "JOIN users u ON p.author_id = u.id " +
            "WHERE p.group_id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "AND p.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY p.author_id, u.username " +
            "ORDER BY posts_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopAuthorsInGroupForPeriod(
            @Param("groupId") Long groupId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("limit") int limit);

    // Средние показатели постов в группе
    @Query(value = "SELECT " +
            "AVG(p.likes_count) as avg_likes, " +
            "AVG(p.comments_count) as avg_comments, " +
            "MAX(p.likes_count) as max_likes, " +
            "MAX(p.comments_count) as max_comments, " +
            "COUNT(p.id) as total_posts " +
            "FROM group_posts p " +
            "WHERE p.group_id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "AND p.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Object[] getGroupPostStatistics(
            @Param("groupId") Long groupId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // Статистика постов пользователя в группе
    @Query(value = "SELECT " +
            "COUNT(p.id) as total_posts, " +
            "AVG(p.likes_count) as avg_likes, " +
            "AVG(p.comments_count) as avg_comments, " +
            "SUM(p.likes_count) as total_likes, " +
            "SUM(p.comments_count) as total_comments, " +
            "MAX(p.likes_count) as max_likes_post " +
            "FROM group_posts p " +
            "WHERE p.group_id = :groupId " +
            "AND p.author_id = :authorId " +
            "AND p.status = 'PUBLISHED'", nativeQuery = true)
    Object[] getUserPostStatisticsInGroup(
            @Param("groupId") Long groupId,
            @Param("authorId") Long authorId);


    // Посты за определенный месяц
    @Query(value = "SELECT p.* FROM group_posts p " +
            "WHERE p.group_id = :groupId " +
            "AND p.status = 'PUBLISHED' " +
            "AND EXTRACT(YEAR FROM p.created_at) = :year " +
            "AND EXTRACT(MONTH FROM p.created_at) = :month " +
            "ORDER BY p.created_at DESC", nativeQuery = true)
    Page<GroupPostEntity> findByMonth(
            @Param("groupId") Long groupId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable);
}