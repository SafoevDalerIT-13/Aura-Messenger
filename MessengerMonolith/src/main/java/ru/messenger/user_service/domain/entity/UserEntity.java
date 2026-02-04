package ru.messenger.user_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.messenger.user_service.contact_service.ContactEntity;
import ru.messenger.user_service.mycontact_service.ContactStatus;
import ru.messenger.user_service.mycontact_service.UserContactEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "login", unique = true, nullable = false)
    private String login;

    @Column(name = "username",nullable = false)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "description",length = 500)
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContactEntity> contacts = new HashSet<>();


    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = false)
    @Builder.Default
    private Set<UserContactEntity> myContacts = new HashSet<>();


    @OneToMany(mappedBy = "friend", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = false)
    @Builder.Default
    private Set<UserContactEntity> addedBy = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    public boolean hasContactInfo() {
        return email != null || phoneNumber != null;
    }

    public void addFriend(UserEntity friend, String nickname) {
        UserContactEntity contact = new UserContactEntity();
        contact.setUser(this);
        contact.setFriend(friend);
        contact.setNickname(nickname);
        contact.setStatus(ContactStatus.PENDING);

        this.myContacts.add(contact);
        friend.getAddedBy().add(contact);
    }

    public boolean isFriend(UserEntity other) {
        return this.myContacts.stream()
                .anyMatch(contact ->
                        contact.getFriend().equals(other) &&
                                contact.isAccepted()
                );
    }

}
