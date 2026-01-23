package ru.messenger.user_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

}
