package ru.messenger.user_service.mycontact_service;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_contacts_user_friend",
                columnNames = {"user_id", "friend_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Пользователь, который добавляет в контакты
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Пользователь, которого добавляют в контакты
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private UserEntity friend;

    // Псевдоним (кастомное имя для друга)
    private String nickname;

    // Статус заявки
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContactStatus status = ContactStatus.PENDING;

    // Флаги
    @Column(name = "is_favorite")
    private boolean isFavorite = false;

    @Column(name = "is_blocked")
    private boolean isBlocked = false;

    // Временные метки
    @CreationTimestamp
    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    // Вспомогательный метод для проверки
    public boolean isAccepted() {
        return status == ContactStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == ContactStatus.PENDING;
    }
}