package ru.messenger.user_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByPhoneNumber(String phone);
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM UserEntity u WHERE u.login = :identifier OR u.email = :identifier OR u.phoneNumber = :identifier")
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);

    default Optional<UserEntity> findByIdentifierWithClean(String identifier) {
        Optional<UserEntity> user = findByLogin(identifier);
        if (user.isPresent()) {
            return user;
        }

        user = findByEmail(identifier);
        if (user.isPresent()) {
            return user;
        }

        String cleanedPhone = cleanPhoneNumber(identifier);
        return findByPhoneNumber(cleanedPhone);
    }

    default String cleanPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String cleaned = phone.replaceAll("[^\\d+]", "");

        if (cleaned.startsWith("8") && cleaned.length() >= 11) {
            cleaned = "+7" + cleaned.substring(1);
        }
        else if (cleaned.startsWith("7") && !cleaned.startsWith("+7")) {
            cleaned = "+" + cleaned;
        }
        else if (cleaned.length() == 10) {
            cleaned = "+7" + cleaned;
        }

        return cleaned;
    }

}
