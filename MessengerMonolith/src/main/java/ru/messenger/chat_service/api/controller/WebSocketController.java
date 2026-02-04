package ru.messenger.chat_service.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import ru.messenger.chat_service.api.dto.*;
import ru.messenger.chat_service.domain.service.ChatService;
import ru.messenger.user_service.domain.service.UserService;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;

    /**
     * Отправка сообщения через WebSocket
     * Client: /app/chat.sendMessage
     * Broadcast: /topic/chat.{chatId}
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload MessageRequestDto requestDto,
            Principal principal) {

        try {
            log.info("WebSocket: отправка сообщения в чат {} от {}",
                    requestDto.getChatId(), principal.getName());

            // Получаем ID пользователя из principal
            var user = userService.getUserByLogin(principal.getName());

            // Сохраняем сообщение через сервис
            MessageResponseDto savedMessage = chatService.sendMessage(requestDto, user.getId());

            // Рассылаем всем участникам чата
            messagingTemplate.convertAndSend(
                    "/topic/chat." + requestDto.getChatId(),
                    savedMessage
            );

            log.info("WebSocket: сообщение {} отправлено в чат {}",
                    savedMessage.getId(), requestDto.getChatId());

        } catch (Exception e) {
            log.error("WebSocket: ошибка отправки сообщения", e);
            // Отправляем ошибку отправителю
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("error", "Не удалось отправить сообщение", "details", e.getMessage())
            );
        }
    }


    /**
     * Уведомление о наборе текста (печатает...)
     * Client: /app/chat.typing
     * Broadcast: /topic/chat.{chatId}.typing
     */
    @MessageMapping("/chat.typing")
    public void sendTypingStatus(
            @Payload TypingStatusRequestDto requestDto,
            Principal principal) {

        try {
            var user = userService.getUserByLogin(principal.getName());

            TypingStatusResponseDto typingStatus = TypingStatusResponseDto.builder()
                    .chatId(requestDto.getChatId())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .isTyping(requestDto.getIsTyping())
                    .timestamp(Instant.now())
                    .build();

            // Рассылаем всем участникам чата, кроме отправителя
            messagingTemplate.convertAndSend(
                    "/topic/chat." + requestDto.getChatId() + ".typing",
                    typingStatus
            );

            log.debug("WebSocket: пользователь {} {} в чате {}",
                    user.getUsername(),
                    requestDto.getIsTyping() ? "печатает" : "перестал печатать",
                    requestDto.getChatId());

        } catch (Exception e) {
            log.error("WebSocket: ошибка отправки статуса печати", e);
        }
    }

    /**
     * Присоединение к чату (пользователь открыл чат)
     * Client: /app/chat.join
     * Broadcast: /topic/chat.{chatId}.presence
     */
    @MessageMapping("/chat.join")
    public void joinChat(
            @Payload JoinChatRequestDto requestDto,
            Principal principal) {

        try {
            var user = userService.getUserByLogin(principal.getName());

            UserPresenceDto presence = UserPresenceDto.builder()
                    .chatId(requestDto.getChatId())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .status("ONLINE")
                    .timestamp(Instant.now())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/chat." + requestDto.getChatId() + ".presence",
                    presence
            );

            log.info("WebSocket: пользователь {} присоединился к чату {}",
                    user.getUsername(), requestDto.getChatId());

        } catch (Exception e) {
            log.error("WebSocket: ошибка присоединения к чату", e);
        }
    }

    /**
     * Приватное сообщение между двумя пользователями
     * Client: /app/chat.private
     * SendToUser: /user/{userId}/queue/messages
     */
    /**
     * Приватное сообщение между двумя пользователями
     */
    @MessageMapping("/chat.private")
    @SendToUser("/queue/messages")
    public MessageResponseDto sendPrivateMessage(
            @Payload PrivateMessageRequestDto requestDto,
            Principal principal) {

        var sender = userService.getUserByLogin(principal.getName());

        // 1. Находим или создаем приватный чат
        ChatResponseDto privateChat = chatService.getOrCreatePrivateChat(
                sender.getId(),
                requestDto.getRecipientId()
        );

        // 2. Создаем сообщение
        MessageRequestDto messageRequest = MessageRequestDto.builder()
                .chatId(privateChat.getId())
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .build();

        MessageResponseDto savedMessage = chatService.sendMessage(messageRequest, sender.getId());

        // 3. Отправляем получателю
        messagingTemplate.convertAndSendToUser(
                requestDto.getRecipientId().toString(),
                "/queue/messages",
                savedMessage
        );

        // 4. И в общий топик чата
        messagingTemplate.convertAndSend(
                "/topic/chat." + privateChat.getId(),
                savedMessage
        );

        return savedMessage;
    }

        /**
         * Удаление сообщения
         * Client: /app/chat.deleteMessage
         * Broadcast: /topic/chat.{chatId}.delete
         */
    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(
            @Payload DeleteMessageRequestDto requestDto,
            Principal principal) {

        try {
            var user = userService.getUserByLogin(principal.getName());

            log.info("WebSocket: удаление сообщения {} пользователем {}",
                    requestDto.getMessageId(), user.getUsername());

            // Удаляем через сервис
            chatService.deleteMessage(requestDto.getMessageId());

            // Уведомляем всех в чате
            Map<String, Object> deleteEvent = Map.of(
                    "type", "MESSAGE_DELETED",
                    "messageId", requestDto.getMessageId(),
                    "deletedBy", user.getId(),
                    "timestamp", Instant.now()
            );

            messagingTemplate.convertAndSend(
                    "/topic/chat." + requestDto.getChatId() + ".events",
                    deleteEvent
            );

        } catch (Exception e) {
            log.error("WebSocket: ошибка удаления сообщения", e);
        }
    }
}