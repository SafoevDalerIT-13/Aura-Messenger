package ru.messenger.chat_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.messenger.chat_service.domain.entity.ChatEntity;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    // Найти все чаты пользователя
    @Query("SELECT c FROM ChatEntity c JOIN c.participants p WHERE p.id = :userId")
    Page<ChatEntity> findAllByUserId(@Param("userId") Long userId, Pageable pageable);


    // Найти чаты с непрочитанными сообщениями
    @Query("SELECT c FROM ChatEntity c JOIN c.participants p WHERE p.id = :userId AND " +
            "EXISTS (SELECT 1 FROM MessageEntity m WHERE m.chat = c AND " +
            "m.status = 'SENT' AND :userId NOT IN elements(m.readBy))")
    List<ChatEntity> findChatsWithUnreadMessages(@Param("userId") Long userId);

    // Найти приватный чат между двумя пользователями
    @Query("SELECT c FROM ChatEntity c " +
            "WHERE c.type = 'PRIVATE' " +
            "AND EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :userId1) " +
            "AND EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :userId2) " +
            "AND size(c.participants) = 2")
    Optional<ChatEntity> findPrivateChat(@Param("userId1") Long userId1,
                                         @Param("userId2") Long userId2);

    // Проверить, является ли пользователь участником чата
    @Query("SELECT COUNT(c) > 0 FROM ChatEntity c " +
            "JOIN c.participants p " +
            "WHERE c.id = :chatId AND p.id = :userId")
    boolean isUserParticipant(@Param("chatId") Long chatId,
                              @Param("userId") Long userId);


}