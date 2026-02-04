package ru.messenger.chat_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.messenger.chat_service.domain.entity.MessageEntity;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    // УПРОЩЕННЫЙ запрос - убираем все JOIN FETCH
    @Query("SELECT m FROM MessageEntity m WHERE m.chat.id = :chatId ORDER BY m.sentAt DESC")
    Page<MessageEntity> findByChatIdOrderBySentAtDesc(@Param("chatId") Long chatId, Pageable pageable);

    // Метод для пометки сообщений как прочитанных
    @Modifying
    @Query("UPDATE MessageEntity m SET m.status = 'READ' " +
            "WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.status != 'READ'")
    void markMessagesAsReadForUser(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Получить новые сообщения
    @Query("SELECT m FROM MessageEntity m " +
            "LEFT JOIN FETCH m.sender " +
            "WHERE m.chat.id = :chatId AND m.sentAt > :since " +
            "ORDER BY m.sentAt ASC")
    List<MessageEntity> findNewMessages(@Param("chatId") Long chatId,
                                        @Param("since") Instant since);

    // Получить непрочитанные сообщения - ИСПРАВЛЕННЫЙ ЗАПРОС
    @Query("SELECT m FROM MessageEntity m " +
            "WHERE m.chat.id = :chatId " +
            "AND m.sender.id != :userId " +
            "AND NOT EXISTS (SELECT 1 FROM m.readBy r WHERE r = :userId)")
    List<MessageEntity> findUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Альтернативный вариант для непрочитанных сообщений
    @Query("SELECT m FROM MessageEntity m " +
            "WHERE m.chat.id = :chatId " +
            "AND m.sender.id != :userId " +
            "AND m.status != 'READ'")
    List<MessageEntity> findUnreadMessagesByStatus(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Простой вариант - подсчет непрочитанных
    @Query("SELECT COUNT(m) FROM MessageEntity m " +
            "WHERE m.chat.id = :chatId " +
            "AND m.sender.id != :userId " +
            "AND (m.readBy IS EMPTY OR :userId NOT IN (SELECT r FROM m.readBy r))")
    long countUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Оставляем только основные методы:
    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.chat.id = :chatId")
    long countByChatId(@Param("chatId") Long chatId);
}