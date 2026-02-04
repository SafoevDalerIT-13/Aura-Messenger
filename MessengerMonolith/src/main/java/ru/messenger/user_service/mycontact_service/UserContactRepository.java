package ru.messenger.user_service.mycontact_service;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContactRepository extends JpaRepository<UserContactEntity, Long> {
    // Найти контакт по пользователю и другу
    Optional<UserContactEntity> findByUserIdAndFriendId(Long userId, Long friendId);

    // Найти все контакты пользователя
    List<UserContactEntity> findByUserId(Long userId);

    // Найти все, кто добавил пользователя
    List<UserContactEntity> findByFriendId(Long friendId);

    // Найти по ID для определенного пользователя (для проверки прав)
    Optional<UserContactEntity> findByFriendIdAndId(Long friendId, Long contactId);

    // Найти контакты по статусу
    List<UserContactEntity> findByUserIdAndStatus(Long userId, ContactStatus status);

    // Найти контакты по статусу (кто добавил пользователя)
    List<UserContactEntity> findByFriendIdAndStatus(Long friendId, ContactStatus status);

    // Найти избранные контакты
    List<UserContactEntity> findByUserIdAndIsFavoriteTrue(Long userId);

    // Проверить существование контакта
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    // Удалить контакт
    void deleteByUserIdAndFriendId(Long userId, Long friendId);

    // Поиск по нику/username
    @Query("SELECT uc FROM UserContactEntity uc WHERE uc.user.id = :userId " +
            "AND (LOWER(uc.nickname) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(uc.friend.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(uc.friend.login) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND uc.status = 'ACCEPTED'")
    List<UserContactEntity> searchFriends(@Param("userId") Long userId, @Param("query") String query);
    // Новые методы для сервиса:
    Optional<UserContactEntity> findByUserIdAndFriendIdAndStatus(Long userId, Long friendId, ContactStatus status);

    long countByUserIdAndStatus(Long userId, ContactStatus status);

    long countByFriendIdAndStatus(Long friendId, ContactStatus status);
}
