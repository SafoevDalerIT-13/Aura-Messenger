package ru.messenger.user_service.mycontact_service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.messenger.user_service.domain.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserContactController {

    private final UserContactService userContactService;
    private final UserService userService;

    // ==== ПОИСК ПОЛЬЗОВАТЕЛЕЙ ====

    // Поиск пользователей для добавления в друзья
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponseDto>> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = userService.getCurrentUserId(userDetails);
        List<UserSearchResponseDto> users = userContactService.searchUsersForAdding(currentUserId, query);

        return ResponseEntity.ok(users);
    }

    // ==== УПРАВЛЕНИЕ ДРУЗЬЯМИ ====

    // Получить моих друзей
    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<UserContactResponseDto>> getFriends(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserContactResponseDto> friends = userContactService.getMyFriends(userId);
        return ResponseEntity.ok(friends);
    }

    // Поиск среди друзей
    @GetMapping("/{userId}/friends/search")
    public ResponseEntity<List<UserContactResponseDto>> searchFriends(
            @PathVariable Long userId,
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserContactResponseDto> friends = userContactService.searchFriends(userId, query);
        return ResponseEntity.ok(friends);
    }

    // ==== УПРАВЛЕНИЕ ЗАЯВКАМИ ====

    // Получить входящие заявки
    @GetMapping("/{userId}/friends/requests/incoming")
    public ResponseEntity<List<UserContactResponseDto>> getIncomingRequests(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserContactResponseDto> requests = userContactService.getIncomingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    // Получить исходящие заявки
    @GetMapping("/{userId}/friends/requests/outgoing")
    public ResponseEntity<List<UserContactResponseDto>> getOutgoingRequests(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserContactResponseDto> requests = userContactService.getOutgoingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    // ==== ДЕЙСТВИЯ С ЗАЯВКАМИ ====

    // Отправить заявку в друзья
    @PostMapping("/{userId}/friends")
    public ResponseEntity<UserContactResponseDto> sendFriendRequest(
            @PathVariable Long userId,
            @Valid @RequestBody UserContactRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) throws BadRequestException {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserContactResponseDto response = userContactService.sendFriendRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Принять заявку в друзья
    @PostMapping("/{userId}/friends/requests/{contactId}/accept")
    public ResponseEntity<UserContactResponseDto> acceptFriendRequest(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails userDetails) throws BadRequestException {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserContactResponseDto response = userContactService.acceptFriendRequest(userId, contactId);
        return ResponseEntity.ok(response);
    }

    // Отклонить заявку в друзья
    @PostMapping("/{userId}/friends/requests/{contactId}/reject")
    public ResponseEntity<UserContactResponseDto> rejectFriendRequest(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails userDetails) throws BadRequestException {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserContactResponseDto response = userContactService.rejectFriendRequest(userId, contactId);
        return ResponseEntity.ok(response);
    }

    // Отменить свою заявку
    @DeleteMapping("/{userId}/friends/requests/{contactId}")
    public ResponseEntity<Void> cancelFriendRequest(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails userDetails) throws BadRequestException {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userContactService.cancelFriendRequest(userId, contactId);
        return ResponseEntity.noContent().build();
    }

    // ==== ДЕЙСТВИЯ С ДРУЗЬЯМИ ====

    // Удалить из друзей
    @DeleteMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable Long userId,
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) throws BadRequestException {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userContactService.removeFriend(userId, friendId);
        return ResponseEntity.noContent().build();
    }

    // Добавить друга в избранное
    @PostMapping("/{userId}/friends/{friendId}/favorite")
    public ResponseEntity<UserContactResponseDto> addToFavorites(
            @PathVariable Long userId,
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) throws BadRequestException {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserContactResponseDto response = userContactService.addToFavorites(userId, friendId);
        return ResponseEntity.ok(response);
    }

    // ==== ПРОВЕРКА СТАТУСА ====

    // Проверить статус отношений с пользователем
    @GetMapping("/{userId}/friends/status/{targetUserId}")
    public ResponseEntity<FriendshipStatusResponseDto> getFriendshipStatus(
            @PathVariable Long userId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FriendshipStatusResponseDto status = userContactService.getFriendshipStatus(userId, targetUserId);
        return ResponseEntity.ok(status);
    }

    // ==== СТАТИСТИКА ====

    // Получить количество друзей
    @GetMapping("/{userId}/friends/count")
    public ResponseEntity<Map<String, Object>> getFriendsCount(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        long count = userContactService.getFriendsCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Проверить, являются ли пользователи друзьями
    @GetMapping("/{userId}/friends/check/{friendId}")
    public ResponseEntity<Map<String, Object>> checkIfFriends(
            @PathVariable Long userId,
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!userService.isOwner(userId, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean areFriends = userContactService.areFriends(userId, friendId);
        return ResponseEntity.ok(Map.of("areFriends", areFriends));
    }
}