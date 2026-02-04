package ru.messenger.chat_service.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.messenger.chat_service.api.dto.ChatResponseDto;
import ru.messenger.chat_service.domain.entity.ChatEntity;
import ru.messenger.user_service.domain.entity.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "participantIds", source = "participants", qualifiedByName = "mapParticipantsToIds")
    @Mapping(target = "lastMessageId", source = "lastMessageId")
    ChatResponseDto toResponseDto(ChatEntity chat);

    @Named("mapParticipantsToIds")
    default Set<Long> mapParticipantsToIds(Set<UserEntity> participants) {
        if (participants == null) {
            return Set.of();
        }
        return participants.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
    }
}