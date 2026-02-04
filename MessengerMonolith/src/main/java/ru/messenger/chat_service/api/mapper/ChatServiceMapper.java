package ru.messenger.chat_service.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.messenger.chat_service.api.dto.*;
import ru.messenger.chat_service.domain.entity.*;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ChatServiceMapper {

    // === CHAT MAPPINGS ===

    @Mapping(target = "participantIds", source = "participants", qualifiedByName = "mapParticipantsToIds")
    @Mapping(target = "participants", source = "participants", qualifiedByName = "mapParticipantsToDtos")
    @Mapping(target = "lastMessageId", source = "lastMessageId")
    ChatResponseDto toChatResponseDto(ChatEntity chat);

    @org.mapstruct.Named("mapParticipantsToIds")
    default Set<Long> mapParticipantsToIds(Set<UserEntity> participants) {
        if (participants == null) {
            return Set.of();
        }
        return participants.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
    }

    @org.mapstruct.Named("mapParticipantsToDtos")
    default List<ChatParticipantDto> mapParticipantsToDtos(Set<UserEntity> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
                .map(this::toChatParticipantDto)
                .collect(Collectors.toList());
    }

    // Вспомогательный метод для маппинга участника
    default ChatParticipantDto toChatParticipantDto(UserEntity user) {
        if (user == null) {
            return null;
        }
        return ChatParticipantDto.builder()
                .id(user.getId())
                .username(user.getUsername() != null ? user.getUsername() : user.getLogin())
                .login(user.getLogin())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .description(user.getDescription())
                .build();
    }

    // Делаем List mapping явным
    default List<ChatResponseDto> toChatResponseDtoList(List<ChatEntity> chats) {
        if (chats == null) {
            return null;
        }
        return chats.stream()
                .map(this::toChatResponseDto)
                .collect(Collectors.toList());
    }

    // === MESSAGE MAPPINGS ===

    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", expression = "java(message.getSender() != null ? (message.getSender().getUsername() != null ? message.getSender().getUsername() : message.getSender().getLogin()) : \"Неизвестный\")")
    MessageResponseDto toMessageResponseDto(MessageEntity message);

    // Делаем List mapping явным
    default List<MessageResponseDto> toMessageResponseDtoList(List<MessageEntity> messages) {
        if (messages == null) {
            return null;
        }
        return messages.stream()
                .map(this::toMessageResponseDto)
                .collect(Collectors.toList());
    }

    // === ATTACHMENT MAPPING ===

    @Mapping(target = "messageId", source = "message.id")
    AttachmentResponseDto toAttachmentResponseDto(AttachmentEntity attachment);
}