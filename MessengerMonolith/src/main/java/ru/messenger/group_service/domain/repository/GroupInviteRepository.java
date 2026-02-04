package ru.messenger.group_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.group_service.domain.entity.GroupInviteEntity;
import ru.messenger.group_service.domain.entity.enums.GroupInviteStatus;

import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInviteEntity, Long> {

    Page<GroupInviteEntity> findByGroupId(Long groupId, Pageable pageable);
    Page<GroupInviteEntity> findByInviterId(Long inviterId, Pageable pageable);
    Page<GroupInviteEntity> findByInvitedId(Long invitedId, Pageable pageable);

    Optional<GroupInviteEntity> findByToken(String token);

    @Query("SELECT i FROM GroupInviteEntity i " +
            "WHERE i.group.id = :groupId " +
            "AND i.invited.id = :invitedId " +
            "AND i.status = :status")
    Optional<GroupInviteEntity> findByGroupIdAndInvitedIdAndStatus(
            @Param("groupId") Long groupId,
            @Param("invitedId") Long invitedId,
            @Param("status") GroupInviteStatus status);

    @Query("SELECT i FROM GroupInviteEntity i " +
            "WHERE i.group.id = :groupId " +
            "AND i.invitedEmail = :invitedEmail " +
            "AND i.status = :status")
    Optional<GroupInviteEntity> findByGroupIdAndInvitedEmailAndStatus(
            @Param("groupId") Long groupId,
            @Param("invitedEmail") String invitedEmail,
            @Param("status") GroupInviteStatus status);

    long countByGroupIdAndStatus(Long groupId, GroupInviteStatus status);
}