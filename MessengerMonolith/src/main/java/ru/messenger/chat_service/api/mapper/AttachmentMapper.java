package ru.messenger.chat_service.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.messenger.chat_service.api.dto.AttachmentResponseDto;
import ru.messenger.chat_service.domain.entity.AttachmentEntity;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(target = "messageId", source = "message.id")
    AttachmentResponseDto toResponseDto(AttachmentEntity attachment);
}