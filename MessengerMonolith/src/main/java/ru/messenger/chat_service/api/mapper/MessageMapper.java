package ru.messenger.chat_service.api.mapper;

import org.mapstruct.*;
import ru.messenger.chat_service.api.dto.AttachmentResponseDto;
import ru.messenger.chat_service.api.dto.MessageResponseDto;
import ru.messenger.chat_service.domain.entity.AttachmentEntity;
import ru.messenger.chat_service.domain.entity.MessageEntity;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AttachmentMapper.class})
public interface MessageMapper {

    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "attachments", source = "attachments")
    MessageResponseDto toResponseDto(MessageEntity message);

    @AfterMapping
    default void setMessageId(@MappingTarget AttachmentResponseDto attachmentDto, MessageEntity message) {
        attachmentDto.setMessageId(message.getId());
    }
}