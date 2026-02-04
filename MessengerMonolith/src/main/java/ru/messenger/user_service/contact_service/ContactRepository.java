package ru.messenger.user_service.contact_service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity,Long> {
    // Найти все контакты пользователя
    List<ContactEntity> findByOwnerId(Long userId);

    // Найти контакт по ID и владельцу
    Optional<ContactEntity> findByIdAndOwnerId(Long id, Long ownerId);

    // Проверить существование контакта у пользователя
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    // Снять флаг основного со всех контактов пользователя
    @Modifying
    @Query("UPDATE ContactEntity c SET c.isPrimary = false WHERE c.owner.id = :userId")
    void unsetAllPrimary(@Param("userId") Long userId);

    // Установить контакт как основной
    @Modifying
    @Query("UPDATE ContactEntity c SET c.isPrimary = true WHERE c.id = :contactId AND c.owner.id = :userId")
    int setPrimary(@Param("contactId") Long contactId, @Param("userId") Long userId);

    // Найти основной контакт пользователя
    Optional<ContactEntity> findByOwnerIdAndIsPrimaryTrue(Long userId);

    // Найти контакты по типу
    List<ContactEntity> findByOwnerIdAndType(Long userId, String type);
}
