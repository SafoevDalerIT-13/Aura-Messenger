package ru.messenger.user_service.mycontact_service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.user_service.contact_service.ContactNotFoundException;
import ru.messenger.user_service.domain.entity.UserEntity;

import ru.messenger.user_service.domain.repository.UserRepository;
import ru.messenger.user_service.domain.service.exception.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserContactService {

    private final UserContactRepository userContactRepository;
    private final UserRepository userRepository;
    private final UserContactMapper userContactMapper;

    // ==== Основные методы ====

    // Отправить заявку в друзья
    public UserContactResponseDto sendFriendRequest(Long userId, UserContactRequestDto request) throws BadRequestException {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            UserEntity friend = userRepository.findById(request.getFriendId())
                    .orElseThrow(() -> new FriendNotFoundException("Friend not found"));

            // Проверка: нельзя добавить себя
            if (userId.equals(request.getFriendId())) {
                throw new BadRequestException("Cannot add yourself as a friend");
            }

            // Проверка: не существует ли уже такой связи
            Optional<UserContactEntity> existingContact = userContactRepository
                    .findByUserIdAndFriendId(userId, request.getFriendId());

            if (existingContact.isPresent()) {
                throw new BadRequestException("Friend request already exists");
            }

            // Создаем заявку
            UserContactEntity contact = new UserContactEntity();
            contact.setUser(user);
            contact.setFriend(friend);
            contact.setNickname(request.getNickname());
            contact.setStatus(ContactStatus.PENDING);

            UserContactEntity saved = userContactRepository.save(contact);
            log.info("Friend request sent from user {} to user {}", userId, request.getFriendId());

            return userContactMapper.toResponse(saved);

        } catch (Exception e) {
            log.error("Error sending friend request: {}", e.getMessage());
            throw new BadRequestException("Failed to send friend request: " + e.getMessage());
        }
    }

    // Принять заявку в друзья
    public UserContactResponseDto acceptFriendRequest(Long userId, Long contactId) throws BadRequestException {
        try {
            UserContactEntity contact = userContactRepository.findByFriendIdAndId(userId, contactId)
                    .orElseThrow(() -> new FriendNotFoundException("Friend request not found"));

            if (contact.getStatus() != ContactStatus.PENDING) {
                throw new BadRequestException("Request is not pending");
            }

            contact.setStatus(ContactStatus.ACCEPTED);
            contact.setLastInteractionAt(LocalDateTime.now());

            UserContactEntity updated = userContactRepository.save(contact);
            log.info("Friend request accepted by user {} from user {}", userId, contact.getUser().getId());

            return userContactMapper.toResponse(updated);

        } catch (Exception e) {
            log.error("Error accepting friend request: {}", e.getMessage());
            throw new BadRequestException("Failed to accept friend request: " + e.getMessage());
        }
    }

    // Отклонить заявку в друзья
    public UserContactResponseDto rejectFriendRequest(Long userId, Long contactId) throws BadRequestException {
        try {
            UserContactEntity contact = userContactRepository.findByFriendIdAndId(userId, contactId)
                    .orElseThrow(() -> new FriendNotFoundException("Friend request not found"));

            contact.setStatus(ContactStatus.REJECTED);

            UserContactEntity updated = userContactRepository.save(contact);
            log.info("Friend request rejected by user {} from user {}", userId, contact.getUser().getId());

            return userContactMapper.toResponse(updated);

        } catch (Exception e) {
            log.error("Error rejecting friend request: {}", e.getMessage());
            throw new BadRequestException("Failed to reject friend request: " + e.getMessage());
        }
    }

    // Отменить свою заявку
    public void cancelFriendRequest(Long userId, Long contactId) throws BadRequestException {
        try {
            UserContactEntity contact = userContactRepository.findById(contactId)
                    .orElseThrow(() -> new ContactNotFoundException("Contact not found"));

            // Проверяем, что это действительно заявка от этого пользователя
            if (!contact.getUser().getId().equals(userId)) {
                throw new BadRequestException("Cannot cancel another user's request");
            }

            if (contact.getStatus() != ContactStatus.PENDING) {
                throw new BadRequestException("Request is not pending");
            }

            userContactRepository.delete(contact);
            log.info("User {} cancelled friend request to user {}", userId, contact.getFriend().getId());

        } catch (Exception e) {
            log.error("Error cancelling friend request: {}", e.getMessage());
            throw new BadRequestException("Failed to cancel friend request: " + e.getMessage());
        }
    }

    // Удалить из друзей
    public void removeFriend(Long userId, Long friendId) throws BadRequestException {
        try {
            UserContactEntity contact = userContactRepository.findByUserIdAndFriendId(userId, friendId)
                    .orElseThrow(() -> new ContactNotFoundException("Contact not found"));

            userContactRepository.delete(contact);
            log.info("User {} removed friend {}", userId, friendId);

        } catch (Exception e) {
            log.error("Error removing friend: {}", e.getMessage());
            throw new BadRequestException("Failed to remove friend: " + e.getMessage());
        }
    }

    // Добавить в избранное
    public UserContactResponseDto addToFavorites(Long userId, Long friendId) throws BadRequestException {
        try {
            UserContactEntity contact = userContactRepository.findByUserIdAndFriendId(userId, friendId)
                    .orElseThrow(() -> new ContactNotFoundException("Contact not found"));

            if (contact.getStatus() != ContactStatus.ACCEPTED) {
                throw new BadRequestException("Only accepted friends can be added to favorites");
            }

            contact.setFavorite(true);
            UserContactEntity updated = userContactRepository.save(contact);

            return userContactMapper.toResponse(updated);

        } catch (Exception e) {
            log.error("Error adding to favorites: {}", e.getMessage());
            throw new BadRequestException("Failed to add to favorites: " + e.getMessage());
        }
    }

    // ==== Получение данных ====

    // Получить список моих друзей (ТОЛЬКО ПРИНЯТЫЕ заявки)
    public List<UserContactResponseDto> getMyFriends(Long userId) {
        try {
            List<UserContactEntity> contacts = userContactRepository
                    .findByUserIdAndStatus(userId, ContactStatus.ACCEPTED);

            // Также получаем друзей, которые приняли мои заявки
            List<UserContactEntity> receivedContacts = userContactRepository
                    .findByFriendIdAndStatus(userId, ContactStatus.ACCEPTED);

            // Объединяем списки
            Set<UserContactEntity> allFriends = new HashSet<>();
            allFriends.addAll(contacts);
            allFriends.addAll(receivedContacts);

            return allFriends.stream()
                    .map(userContactMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting friends for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Получить входящие заявки
    public List<UserContactResponseDto> getIncomingRequests(Long userId) {
        try {
            List<UserContactEntity> contacts = userContactRepository
                    .findByFriendIdAndStatus(userId, ContactStatus.PENDING);

            return contacts.stream()
                    .map(userContactMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting incoming requests for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Получить исходящие заявки
    public List<UserContactResponseDto> getOutgoingRequests(Long userId) {
        try {
            List<UserContactEntity> contacts = userContactRepository
                    .findByUserIdAndStatus(userId, ContactStatus.PENDING);

            return contacts.stream()
                    .map(userContactMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting outgoing requests for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Поиск среди друзей
    public List<UserContactResponseDto> searchFriends(Long userId, String query) {
        try {
            List<UserContactEntity> contacts = userContactRepository.searchFriends(userId, query);
            return contacts.stream()
                    .map(userContactMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching friends: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==== Поиск и статусы ====

    // Поиск пользователей для добавления в друзья
    public List<UserSearchResponseDto> searchUsersForAdding(Long userId, String searchQuery) {
        try {
            List<UserEntity> users = userRepository.searchUsersExcluding(searchQuery, userId);

            return users.stream()
                    .map(user -> {
                        // Проверяем статус отношений
                        Map<String, Object> status = checkFriendshipStatus(userId, user.getId());

                        return UserSearchResponseDto.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .login(user.getLogin())
                                .email(user.getEmail())
                                .avatarUrl(user.getAvatarUrl())
                                .friendshipStatus((String) status.get("status"))
                                .contactId(status.containsKey("contactId") ?
                                        ((Number) status.get("contactId")).longValue() : null)
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // Получить статус отношений (для API)
    public FriendshipStatusResponseDto getFriendshipStatus(Long userId, Long targetUserId) {
        try {
            // Проверяем, отправил ли я заявку
            Optional<UserContactEntity> myRequest = userContactRepository
                    .findByUserIdAndFriendId(userId, targetUserId);

            if (myRequest.isPresent()) {
                return FriendshipStatusResponseDto.builder()
                        .status(myRequest.get().getStatus() == ContactStatus.PENDING ?
                                "PENDING_OUTGOING" : myRequest.get().getStatus().name())
                        .contactId(myRequest.get().getId())
                        .requestedAt(myRequest.get().getAddedAt())
                        .nickname(myRequest.get().getNickname())
                        .build();
            }

            // Проверяем, отправил ли мне заявку этот пользователь
            Optional<UserContactEntity> theirRequest = userContactRepository
                    .findByUserIdAndFriendId(targetUserId, userId);

            if (theirRequest.isPresent()) {
                return FriendshipStatusResponseDto.builder()
                        .status(theirRequest.get().getStatus() == ContactStatus.PENDING ?
                                "PENDING_INCOMING" : theirRequest.get().getStatus().name())
                        .contactId(theirRequest.get().getId())
                        .requestedAt(theirRequest.get().getAddedAt())
                        .build();
            }

            return FriendshipStatusResponseDto.builder()
                    .status("NONE")
                    .build();

        } catch (Exception e) {
            log.error("Error getting friendship status: {}", e.getMessage());
            return FriendshipStatusResponseDto.builder()
                    .status("ERROR")
                    .build();
        }
    }

    // Проверить статус отношений (внутренний метод)
    private Map<String, Object> checkFriendshipStatus(Long userId, Long targetUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Проверяем, отправил ли я заявку
            Optional<UserContactEntity> myRequest = userContactRepository
                    .findByUserIdAndFriendId(userId, targetUserId);

            if (myRequest.isPresent()) {
                result.put("status", myRequest.get().getStatus() == ContactStatus.PENDING ?
                        "PENDING_OUTGOING" : myRequest.get().getStatus().name());
                result.put("direction", "OUTGOING");
                result.put("contactId", myRequest.get().getId());
                return result;
            }

            // Проверяем, отправил ли мне заявку этот пользователь
            Optional<UserContactEntity> theirRequest = userContactRepository
                    .findByUserIdAndFriendId(targetUserId, userId);

            if (theirRequest.isPresent()) {
                result.put("status", theirRequest.get().getStatus() == ContactStatus.PENDING ?
                        "PENDING_INCOMING" : theirRequest.get().getStatus().name());
                result.put("direction", "INCOMING");
                result.put("contactId", theirRequest.get().getId());
                return result;
            }

            result.put("status", "NONE");
            result.put("direction", "NONE");

        } catch (Exception e) {
            log.error("Error checking friendship status: {}", e.getMessage());
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }

        return result;
    }

    // ==== Вспомогательные методы ====

    // Получить информацию о пользователе
    public UserEntity getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // Проверить, являются ли пользователи друзьями
    public boolean areFriends(Long userId1, Long userId2) {
        try {
            // Проверяем в обе стороны
            Optional<UserContactEntity> contact1 = userContactRepository
                    .findByUserIdAndFriendIdAndStatus(userId1, userId2, ContactStatus.ACCEPTED);

            Optional<UserContactEntity> contact2 = userContactRepository
                    .findByUserIdAndFriendIdAndStatus(userId2, userId1, ContactStatus.ACCEPTED);

            return contact1.isPresent() || contact2.isPresent();

        } catch (Exception e) {
            log.error("Error checking friendship: {}", e.getMessage());
            return false;
        }
    }

    // Получить количество друзей
    public long getFriendsCount(Long userId) {
        try {
            // Количество друзей (принятые заявки в обе стороны)
            long outgoingFriends = userContactRepository
                    .countByUserIdAndStatus(userId, ContactStatus.ACCEPTED);

            long incomingFriends = userContactRepository
                    .countByFriendIdAndStatus(userId, ContactStatus.ACCEPTED);

            return outgoingFriends + incomingFriends;

        } catch (Exception e) {
            log.error("Error getting friends count: {}", e.getMessage());
            return 0;
        }
    }

    // Обновить время последнего взаимодействия
    public void updateLastInteraction(Long userId, Long friendId) {
        try {
            Optional<UserContactEntity> contact = userContactRepository
                    .findByUserIdAndFriendId(userId, friendId);

            if (contact.isPresent() && contact.get().getStatus() == ContactStatus.ACCEPTED) {
                contact.get().setLastInteractionAt(LocalDateTime.now());
                userContactRepository.save(contact.get());
            }

        } catch (Exception e) {
            log.error("Error updating last interaction: {}", e.getMessage());
        }
    }
}
