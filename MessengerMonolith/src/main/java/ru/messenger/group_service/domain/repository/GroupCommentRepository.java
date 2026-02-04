package ru.messenger.group_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.group_service.domain.entity.GroupPostCommentEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupCommentRepository extends JpaRepository<GroupPostCommentEntity, Long> {

    // Основные методы
    Page<GroupPostCommentEntity> findByPostId(Long postId, Pageable pageable);
    Page<GroupPostCommentEntity> findByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);
    Page<GroupPostCommentEntity> findByParentCommentId(Long parentCommentId, Pageable pageable);

    // Поиск по автору
    Page<GroupPostCommentEntity> findByAuthorId(Long authorId, Pageable pageable);

    // Подсчеты
    long countByPostId(Long postId);
    long countByAuthorId(Long authorId);
    long countByParentCommentId(Long parentCommentId);

    // Поиск по группе
    @Query("SELECT c FROM GroupPostCommentEntity c " +
            "JOIN c.post p " +
            "WHERE p.group.id = :groupId " +
            "ORDER BY c.createdAt DESC")
    Page<GroupPostCommentEntity> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query("SELECT c FROM GroupPostCommentEntity c " +
            "JOIN c.post p " +
            "WHERE p.group.id = :groupId " +
            "AND c.author.id = :authorId " +
            "ORDER BY c.createdAt DESC")
    Page<GroupPostCommentEntity> findByGroupIdAndAuthorId(@Param("groupId") Long groupId,
                                                          @Param("authorId") Long authorId,
                                                          Pageable pageable);

    // Статистика по датам
    @Query("SELECT COUNT(c) FROM GroupPostCommentEntity c " +
            "JOIN c.post p " +
            "WHERE p.group.id = :groupId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByGroupIdAndCreatedAtBetween(@Param("groupId") Long groupId,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(c) FROM GroupPostCommentEntity c " +
            "JOIN c.post p " +
            "WHERE p.group.id = :groupId " +
            "AND c.author.id = :authorId")
    long countByGroupIdAndAuthorId(@Param("groupId") Long groupId,
                                   @Param("authorId") Long authorId);

    // ВАЖНО: добавлен этот метод
    @Query("SELECT COUNT(c) FROM GroupPostCommentEntity c " +
            "JOIN c.post p " +
            "WHERE p.group.id = :groupId " +
            "AND c.author.id = :authorId " +
            "AND c.createdAt > :since")
    long countByGroupIdAndAuthorIdAndCreatedAtAfter(
            @Param("groupId") Long groupId,
            @Param("authorId") Long authorId,
            @Param("since") Instant since);

    @Query("SELECT COUNT(c) FROM GroupPostCommentEntity c " +
            "WHERE c.createdAt > :since")
    long countByCreatedAtAfter(@Param("since") Instant since);

    // Для статистики по дням
    @Query(value = "SELECT DATE(c.created_at) as date, COUNT(c.id) as count " +
            "FROM group_post_comments c " +
            "JOIN group_posts p ON c.post_id = p.id " +
            "WHERE p.group_id = :groupId " +
            "AND c.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(c.created_at) " +
            "ORDER BY DATE(c.created_at)", nativeQuery = true)
    List<Object[]> countCommentsByDay(@Param("groupId") Long groupId,
                                      @Param("startDate") Instant startDate,
                                      @Param("endDate") Instant endDate);

    // Удаление
    @Modifying
    @Query("DELETE FROM GroupPostCommentEntity c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM GroupPostCommentEntity c WHERE c.parentComment.id = :parentCommentId")
    void deleteByParentCommentId(@Param("parentCommentId") Long parentCommentId);

    // Дополнительные методы для статистики
    @Query("SELECT COUNT(c) FROM GroupPostCommentEntity c " +
            "JOIN c.post p " +
            "WHERE p.group.id = :groupId " +
            "AND c.author.id = :authorId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByGroupIdAndAuthorIdAndCreatedAtBetween(
            @Param("groupId") Long groupId,
            @Param("authorId") Long authorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // Топ активных комментаторов в группе
    @Query(value = "SELECT c.author_id, u.username, COUNT(c.id) as comments_count " +
            "FROM group_post_comments c " +
            "JOIN group_posts p ON c.post_id = p.id " +
            "JOIN users u ON c.author_id = u.id " +
            "WHERE p.group_id = :groupId " +
            "AND c.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY c.author_id, u.username " +
            "ORDER BY comments_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopCommentersInGroup(
            @Param("groupId") Long groupId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("limit") int limit);
}