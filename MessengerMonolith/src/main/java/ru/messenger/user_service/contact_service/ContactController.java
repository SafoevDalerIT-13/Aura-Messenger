package ru.messenger.user_service.contact_service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.messenger.user_service.domain.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;
    private final UserService userService;

    // Получить все контакты пользователя
    @GetMapping
    public ResponseEntity<List<ContactResponseDto>> getUserContacts(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Getting contacts for user ID: {}", userId);

        // Проверка, что пользователь существует
        if (!userService.userExists(userId)) {
            return ResponseEntity.notFound().build();
        }

        // Проверка прав доступа (только владелец)
        if (!userService.isOwner(userId, userDetails.getUsername())) {
            log.warn("Access denied for user {} to contacts of user {}",
                    userDetails.getUsername(), userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ContactResponseDto> contacts = contactService.getUserContacts(userId);
        return ResponseEntity.ok(contacts);
    }

    // Добавить новый контакт
    @PostMapping
    public ResponseEntity<ContactResponseDto> addContact(
            @PathVariable Long userId,
            @Valid @RequestBody ContactRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Adding contact for user ID: {}, request: {}", userId, request);

        // Проверка прав доступа
        if (!userService.isOwner(userId, userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ContactResponseDto response = contactService.addContact(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Обновить контакт
    @PutMapping("/{contactId}")
    public ResponseEntity<ContactResponseDto> updateContact(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @Valid @RequestBody ContactRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating contact ID: {} for user ID: {}", contactId, userId);

        // Проверка прав доступа
        if (!userService.isOwner(userId, userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ContactResponseDto response = contactService.updateContact(userId, contactId, request);
        return ResponseEntity.ok(response);
    }

    // Удалить контакт
    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Deleting contact ID: {} for user ID: {}", contactId, userId);

        // Проверка прав доступа
        if (!userService.isOwner(userId, userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        contactService.deleteContact(userId, contactId);
        return ResponseEntity.noContent().build();
    }

    // Установить основной контакт
    @PatchMapping("/{contactId}/primary")
    public ResponseEntity<Void> setPrimaryContact(
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Setting contact ID: {} as primary for user ID: {}", contactId, userId);

        // Проверка прав доступа
        if (!userService.isOwner(userId, userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        contactService.setPrimaryContact(userId, contactId);
        return ResponseEntity.ok().build();
    }
}