package ru.messenger.group_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.group_service.domain.entity.GroupCommentLikeEntity;

import java.util.Optional;

@Repository
public interface GroupCommentLikeRepository extends JpaRepository<GroupCommentLikeEntity, Long> {

    Optional<GroupCommentLikeEntity> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    @Modifying
    @Query("DELETE FROM GroupCommentLikeEntity l WHERE l.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM GroupCommentLikeEntity l WHERE l.comment.id = :commentId AND l.user.id = :userId")
    void deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM GroupCommentLikeEntity l WHERE l.comment.id = :commentId")
    long countLikesByCommentId(@Param("commentId") Long commentId);
}