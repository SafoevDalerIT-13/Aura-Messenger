package ru.messenger.chat_service.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.chat_service.api.dto.*;
import ru.messenger.chat_service.domain.entity.ChatEntity;
import ru.messenger.chat_service.domain.entity.MessageEntity;
import ru.messenger.chat_service.domain.entity.enums.ChatType;
import ru.messenger.chat_service.domain.entity.enums.MessageStatus;
import ru.messenger.chat_service.api.mapper.ChatServiceMapper;
import ru.messenger.chat_service.domain.repository.ChatRepository;
import ru.messenger.chat_service.domain.repository.MessageRepository;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;
import ru.messenger.user_service.domain.service.exception.UserNotFoundException;

import java.time.Instant;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatServiceMapper chatServiceMapper;

    /**
     * Получить список чатов пользователя
     */
    @Transactional(readOnly = true)
    public Page<ChatResponseDto> getUserChats(Long userId, Pageable pageable) {
        log.info("Получение чатов для пользователя ID: {}", userId);

        Page<ChatEntity> chats = chatRepository.findAllByUserId(userId, pageable);

        return chats.map(chatServiceMapper::toChatResponseDto);
    }

    /**
     * Создать приватный чат
     */
    @Transactional
    public ChatResponseDto createPrivateChat(Long userId1, Long userId2) {
        log.info("Создание приватного чата между пользователями {} и {}", userId1, userId2);

        // Проверяем, что пользователи существуют
        UserEntity user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userId1));

        UserEntity user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userId2));

        // Проверяем, не является ли это одним и тем же пользователем
        if (userId1.equals(userId2)) {
            throw new RuntimeException("Нельзя создать чат с самим собой");
        }

        // Ищем существующий приватный чат
        Optional<ChatEntity> existingChat = chatRepository.findPrivateChat(userId1, userId2);
        if (existingChat.isPresent()) {
            log.info("Приватный чат уже существует: {}", existingChat.get().getId());
            return chatServiceMapper.toChatResponseDto(existingChat.get());
        }

        // Создаем новый чат
        ChatEntity chat = ChatEntity.builder()
                .type(ChatType.PRIVATE)
                .participants(new HashSet<>(Arrays.asList(user1, user2)))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatEntity savedChat = chatRepository.save(chat);
        log.info("Создан новый приватный чат ID: {}", savedChat.getId());

        return chatServiceMapper.toChatResponseDto(savedChat);
    }

    /**
     * Создать групповой чат
     */
    @Transactional
    public ChatResponseDto createGroupChat(ChatRequestDto requestDto, Long creatorId) {
        log.info("Создание группового чата создателем {}", creatorId);

        UserEntity creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Создатель не найден"));

        Set<UserEntity> participants = new HashSet<>();
        participants.add(creator);

        for (Long participantId : requestDto.getParticipantIds()) {
            UserEntity participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new UserNotFoundException("Участник не найден: " + participantId));
            participants.add(participant);
        }

        ChatEntity chat = ChatEntity.builder()
                .name(requestDto.getName())
                .type(requestDto.getType())
                .participants(participants)
                .avatarUrl(requestDto.getAvatarUrl())
                .build();

        ChatEntity savedChat = chatRepository.save(chat);
        log.info("Создан новый чат ID: {}", savedChat.getId());

        return chatServiceMapper.toChatResponseDto(savedChat);
    }

    /**
     * Получить информацию о чате
     */
    @Transactional(readOnly = true)
    public ChatResponseDto getChat(Long chatId, Long userId) {
        log.info("Получение информации о чате ID: {} для пользователя {}", chatId, userId);

        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        // Простая проверка участника
        boolean isParticipant = false;
        for (UserEntity participant : chat.getParticipants()) {
            if (participant.getId().equals(userId)) {
                isParticipant = true;
                break;
            }
        }

        if (!isParticipant) {
            throw new RuntimeException("Доступ к чату запрещен");
        }

        return chatServiceMapper.toChatResponseDto(chat);
    }

    /**
     * Добавить участника в чат
     */
    @Transactional
    public ChatResponseDto addParticipant(ParticipantRequestDto requestDto) {
        log.info("Добавление участника {} в чат {}",
                requestDto.getUserId(), requestDto.getChatId());

        ChatEntity chat = chatRepository.findById(requestDto.getChatId())
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        UserEntity newParticipant = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        chat.getParticipants().add(newParticipant);
        chat.setUpdatedAt(Instant.now());

        ChatEntity updatedChat = chatRepository.save(chat);

        return chatServiceMapper.toChatResponseDto(updatedChat);
    }

    /**
     * Удалить участника из чата
     */
    @Transactional
    public void removeParticipant(ParticipantRequestDto requestDto) {
        log.info("Удаление участника {} из чата {}",
                requestDto.getUserId(), requestDto.getChatId());

        ChatEntity chat = chatRepository.findById(requestDto.getChatId())
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        UserEntity participant = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        chat.getParticipants().remove(participant);
        chat.setUpdatedAt(Instant.now());

        chatRepository.save(chat);
    }

    /**
     * Обновить информацию о чате
     */
    @Transactional
    public ChatResponseDto updateChat(Long chatId, ChatUpdateRequestDto requestDto) {
        log.info("Обновление чата ID: {}", chatId);

        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        if (requestDto.getName() != null) {
            chat.setName(requestDto.getName());
        }

        if (requestDto.getAvatarUrl() != null) {
            chat.setAvatarUrl(requestDto.getAvatarUrl());
        }

        chat.setUpdatedAt(Instant.now());

        ChatEntity updatedChat = chatRepository.save(chat);

        return chatServiceMapper.toChatResponseDto(updatedChat);
    }

    /**
     * Отправить сообщение
     */
    @Transactional
    public MessageResponseDto sendMessage(MessageRequestDto requestDto, Long senderId) {
        log.info("Отправка сообщения в чат {} от пользователя {}",
                requestDto.getChatId(), senderId);

        ChatEntity chat = chatRepository.findById(requestDto.getChatId())
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Отправитель не найден"));

        // Проверка участника
        boolean isParticipant = false;
        for (UserEntity participant : chat.getParticipants()) {
            if (participant.getId().equals(senderId)) {
                isParticipant = true;
                break;
            }
        }

        if (!isParticipant) {
            throw new RuntimeException("Отправитель не является участником чата");
        }

        MessageEntity message = MessageEntity.builder()
                .chat(chat)
                .sender(sender)
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .status(MessageStatus.SENT)
                .sentAt(Instant.now())
                .build();

        MessageEntity savedMessage = messageRepository.save(message);

        chat.setLastMessageId(savedMessage.getId());
        chat.setUpdatedAt(Instant.now());
        chatRepository.save(chat);

        log.info("Сообщение отправлено ID: {}", savedMessage.getId());

        return chatServiceMapper.toMessageResponseDto(savedMessage);
    }


    @Transactional(readOnly = true)
    public Page<MessageResponseDto> getMessages(MessageFilterRequestDto filterDto, Long userId) {
        log.info("Получение сообщений чата {} для пользователя {}",
                filterDto.getChatId(), userId);

        ChatEntity chat = chatRepository.findById(filterDto.getChatId())
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        boolean isParticipant = false;
        for (UserEntity participant : chat.getParticipants()) {
            if (participant.getId().equals(userId)) {
                isParticipant = true;
                break;
            }
        }

        if (!isParticipant) {
            throw new RuntimeException("Доступ к сообщениям запрещен");
        }

        Pageable pageable = Pageable.ofSize(filterDto.getSize())
                .withPage(filterDto.getPage());

        log.debug("Загрузка сообщений для чата {} со страницы {}",
                filterDto.getChatId(), filterDto.getPage());

        Page<MessageEntity> messages = messageRepository
                .findByChatIdOrderBySentAtDesc(filterDto.getChatId(), pageable);

        log.debug("Найдено {} сообщений", messages.getTotalElements());

        return messages.map(message -> {
            try {
                return chatServiceMapper.toMessageResponseDto(message);
            } catch (Exception e) {
                log.error("Ошибка маппинга сообщения {}", message.getId(), e);
                MessageResponseDto dto = new MessageResponseDto();
                dto.setId(message.getId());
                dto.setContent(message.getContent());
                dto.setType(message.getType());
                dto.setStatus(message.getStatus());
                dto.setSentAt(message.getSentAt());

                if (message.getChat() != null) {
                    dto.setChatId(message.getChat().getId());
                }

                if (message.getSender() != null) {
                    dto.setSenderId(message.getSender().getId());
                    String username = message.getSender().getUsername();
                    if (username == null || username.trim().isEmpty()) {
                        username = message.getSender().getLogin();
                    }
                    dto.setSenderUsername(username);
                }

                return dto;
            }
        });
    }

    /**
     * Получить новые сообщения (для WebSocket/лонг-пуллинга)
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDto> getNewMessages(Long chatId, Long userId, Instant since) {
        // Простая проверка участника
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        boolean isParticipant = false;
        for (UserEntity participant : chat.getParticipants()) {
            if (participant.getId().equals(userId)) {
                isParticipant = true;
                break;
            }
        }

        if (!isParticipant) {
            throw new RuntimeException("Доступ запрещен");
        }

        // Упрощаем - берем все сообщения после указанного времени
        List<MessageEntity> allMessages = messageRepository.findAll();
        List<MessageEntity> filteredMessages = new ArrayList<>();

        for (MessageEntity message : allMessages) {
            if (message.getChat() != null &&
                    message.getChat().getId().equals(chatId) &&
                    message.getSentAt() != null &&
                    message.getSentAt().isAfter(since)) {
                filteredMessages.add(message);
            }
        }

        // Сортируем по времени
        filteredMessages.sort((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()));

        List<MessageResponseDto> result = new ArrayList<>();
        for (MessageEntity message : filteredMessages) {
            MessageResponseDto dto = new MessageResponseDto();
            dto.setId(message.getId());
            dto.setContent(message.getContent());
            dto.setType(message.getType());
            dto.setStatus(message.getStatus());
            dto.setSentAt(message.getSentAt());

            if (message.getChat() != null) {
                dto.setChatId(message.getChat().getId());
            }

            if (message.getSender() != null) {
                dto.setSenderId(message.getSender().getId());
                String username = message.getSender().getUsername();
                if (username == null || username.trim().isEmpty()) {
                    username = message.getSender().getLogin();
                }
                dto.setSenderUsername(username);
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * Пометить сообщение как прочитанное
     */
    @Transactional
    public void markMessageAsRead(MarkAsReadRequestDto requestDto) {
        log.info("Пометка сообщения {} как прочитанного пользователем {}",
                requestDto.getMessageId(), requestDto.getUserId());

        MessageEntity message = messageRepository.findById(requestDto.getMessageId())
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        // Простая проверка доступа
        boolean hasAccess = false;
        for (UserEntity participant : message.getChat().getParticipants()) {
            if (participant.getId().equals(requestDto.getUserId())) {
                hasAccess = true;
                break;
            }
        }

        if (!hasAccess) {
            throw new RuntimeException("Доступ запрещен");
        }

        // Просто меняем статус
        message.setStatus(MessageStatus.READ);
        messageRepository.save(message);
    }

    /**
     * Обновить сообщение
     */
    @Transactional
    public MessageResponseDto updateMessage(Long messageId, MessageUpdateRequestDto requestDto) {
        log.info("Обновление сообщения ID: {}", messageId);

        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        if (requestDto.getContent() != null) {
            message.setContent(requestDto.getContent());
        }

        if (requestDto.getStatus() != null) {
            message.setStatus(requestDto.getStatus());
        }

        MessageEntity updatedMessage = messageRepository.save(message);

        return chatServiceMapper.toMessageResponseDto(updatedMessage);
    }

    /**
     * Удалить сообщение
     */
    @Transactional
    public void deleteMessage(Long messageId) {
        log.info("Удаление сообщения ID: {}", messageId);
        messageRepository.deleteById(messageId);
    }

    /**
     * Найти приватный чат между двумя пользователями
     */
    @Transactional(readOnly = true)
    public Optional<ChatResponseDto> findPrivateChat(Long userId1, Long userId2) {
        Optional<ChatEntity> chat = chatRepository.findPrivateChat(userId1, userId2);
        return chat.map(chatServiceMapper::toChatResponseDto);
    }

    /**
     * Найти или создать приватный чат
     */
    @Transactional
    public ChatResponseDto getOrCreatePrivateChat(Long userId1, Long userId2) {
        return findPrivateChat(userId1, userId2)
                .orElseGet(() -> createPrivateChat(userId1, userId2));
    }

    /**
     * Проверить, является ли пользователь участником чата
     */
    @Transactional(readOnly = true)
    public boolean isUserParticipant(Long chatId, Long userId) {
        try {
            ChatEntity chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Чат не найден"));

            for (UserEntity participant : chat.getParticipants()) {
                if (participant.getId().equals(userId)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Ошибка проверки участника чата", e);
            return false;
        }
    }

    /**
     * Простая реализация пометки сообщений как прочитанных
     */
    @Transactional
    public void markMessagesAsReadForUser(Long chatId, Long userId) {
        try {
            List<MessageEntity> messages = messageRepository.findAll();
            for (MessageEntity message : messages) {
                if (message.getChat() != null &&
                        message.getChat().getId().equals(chatId) &&
                        message.getSender() != null &&
                        !message.getSender().getId().equals(userId) &&
                        message.getStatus() != MessageStatus.READ) {

                    message.setStatus(MessageStatus.READ);
                    messageRepository.save(message);
                }
            }
            log.info("Сообщения в чате {} помечены как прочитанные для пользователя {}", chatId, userId);
        } catch (Exception e) {
            log.warn("Не удалось пометить сообщения как прочитанные", e);
        }
    }
}