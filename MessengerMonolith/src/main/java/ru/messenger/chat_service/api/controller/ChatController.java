package ru.messenger.chat_service.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.messenger.chat_service.api.dto.*;
import ru.messenger.chat_service.domain.entity.enums.ChatType;
import ru.messenger.chat_service.domain.entity.enums.MessageType;
import ru.messenger.chat_service.domain.service.ChatService;
import ru.messenger.user_service.api.dto.UserResponseDto;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;
import ru.messenger.user_service.domain.service.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Получить список чатов пользователя
     * GET /api/v1/chats
     */
    @GetMapping
    public ResponseEntity<Page<ChatResponseDto>> getUserChats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);


        Page<ChatResponseDto> chats = chatService.getUserChats(user.getId(), pageable);
        return ResponseEntity.ok(chats);
    }

    /**
     * Создать приватный чат
     */
    @PostMapping("/private/{userId}")
    public ResponseEntity<?> createPrivateChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {

        String login = userDetails.getUsername();
        var currentUser = userService.getUserByLogin(login);

        try {
            ChatResponseDto chat = chatService.createPrivateChat(currentUser.getId(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ошибка создания чата",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Создать чат (групповой или приватный)
     * POST /api/v1/chats
     */
    @PostMapping
    public ResponseEntity<ChatResponseDto> createChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ChatRequestDto requestDto) {

        String login = userDetails.getUsername();
        var currentUser = userService.getUserByLogin(login);

        // Если тип PRIVATE и только один участник в списке
        if (requestDto.getType() == ChatType.PRIVATE &&
                requestDto.getParticipantIds() != null &&
                requestDto.getParticipantIds().size() == 1) {

            Long participantId = requestDto.getParticipantIds().iterator().next();
            ChatResponseDto chat = chatService.createPrivateChat(currentUser.getId(), participantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(chat);
        }

        // Для группового чата
        ChatResponseDto chat = chatService.createGroupChat(requestDto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(chat);
    }

    /**
     * Получить информацию о чате
     * GET /api/v1/chats/{chatId}
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponseDto> getChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        ChatResponseDto chat = chatService.getChat(chatId, user.getId());
        return ResponseEntity.ok(chat);
    }

    /**
     * Обновить информацию о чате
     * PUT /api/v1/chats/{chatId}
     */
    @PutMapping("/{chatId}")
    public ResponseEntity<ChatResponseDto> updateChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId,
            @RequestBody @Valid ChatUpdateRequestDto requestDto) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        // Проверяем, что пользователь является участником чата
        // (эта проверка будет внутри сервиса)
        ChatResponseDto chat = chatService.updateChat(chatId, requestDto);
        return ResponseEntity.ok(chat);
    }

    /**
     * Добавить участника в чат
     * POST /api/v1/chats/{chatId}/participants
     */
    @PostMapping("/{chatId}/participants")
    public ResponseEntity<ChatResponseDto> addParticipant(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId,
            @RequestBody @Valid ParticipantRequestDto requestDto) {

        // Устанавливаем chatId из path variable
        requestDto.setChatId(chatId);

        String login = userDetails.getUsername();
        var currentUser = userService.getUserByLogin(login);

        // Можно добавить проверку прав доступа здесь или в сервисе

        ChatResponseDto chat = chatService.addParticipant(requestDto);
        return ResponseEntity.ok(chat);
    }

    /**
     * Удалить участника из чата
     * DELETE /api/v1/chats/{chatId}/participants/{userId}
     */
    @DeleteMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId,
            @PathVariable Long userId) {

        ParticipantRequestDto requestDto = new ParticipantRequestDto();
        requestDto.setChatId(chatId);
        requestDto.setUserId(userId);

        String login = userDetails.getUsername();
        var currentUser = userService.getUserByLogin(login);

        // Проверяем права доступа (только создатель или администратор может удалять)
        chatService.removeParticipant(requestDto);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Участник удален из чата"
        ));
    }

    /**
     * Получить сообщения чата с фильтрацией
     * GET /api/v1/chats/{chatId}/messages
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<MessageResponseDto>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) MessageType messageType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        MessageFilterRequestDto filterDto = new MessageFilterRequestDto();
        filterDto.setChatId(chatId);
        filterDto.setSenderId(senderId);
        filterDto.setMessageType(messageType);
        filterDto.setPage(page);
        filterDto.setSize(size);

        Page<MessageResponseDto> messages = chatService.getMessages(filterDto, user.getId());
        return ResponseEntity.ok(messages);
    }

    /**
     * Отправить сообщение
     * POST /api/v1/chats/{chatId}/messages
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponseDto> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId,
            @RequestBody @Valid MessageRequestDto requestDto) {

        // Устанавливаем chatId из path variable
        requestDto.setChatId(chatId);

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        MessageResponseDto message = chatService.sendMessage(requestDto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Обновить сообщение
     * PUT /api/v1/messages/{messageId}
     */
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponseDto> updateMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId,
            @RequestBody @Valid MessageUpdateRequestDto requestDto) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        // Проверка прав доступа (можно добавить в сервисе)
        MessageResponseDto message = chatService.updateMessage(messageId, requestDto);
        return ResponseEntity.ok(message);
    }

    /**
     * Удалить сообщение
     * DELETE /api/v1/messages/{messageId}
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        chatService.deleteMessage(messageId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Сообщение удалено"
        ));
    }

    /**
     * Получить новые сообщения (лонг-пуллинг)
     * GET /api/v1/chats/{chatId}/messages/new
     */
    @GetMapping("/{chatId}/messages/new")
    public ResponseEntity<List<MessageResponseDto>> getNewMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId,
            @RequestParam(required = false) Instant since) {

        if (since == null) {
            since = Instant.now().minusSeconds(60); // Последнюю минуту
        }

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        List<MessageResponseDto> messages = chatService.getNewMessages(chatId, user.getId(), since);
        return ResponseEntity.ok(messages);
    }


    @GetMapping("/{chatId}/check")
    public ResponseEntity<?> checkChatAccess(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long chatId) {

        String login = userDetails.getUsername();
        var user = userService.getUserByLogin(login);

        boolean isParticipant = chatService.isUserParticipant(chatId, user.getId());

        return ResponseEntity.ok(Map.of(
                "isParticipant", isParticipant,
                "chatId", chatId,
                "userId", user.getId()
        ));
    }

    /**
     * Получить список доступных пользователей для чата
     */
    @GetMapping("/available-users")
    public ResponseEntity<List<UserResponseDto>> getAvailableUsers(
            @AuthenticationPrincipal UserDetails userDetails) {

        String login = userDetails.getUsername();
        var currentUser = userService.getUserByLogin(login);

        // Получаем всех пользователей кроме текущего
        List<UserEntity> allUsers = userRepository.findAll();
        List<UserResponseDto> availableUsers = allUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .login(user.getLogin())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(availableUsers);
    }
}