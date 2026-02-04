package ru.messenger.user_service.mycontact_service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserContactMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "friendId", source = "friend.id")
    @Mapping(target = "friendUsername", source = "friend.username")
    @Mapping(target = "friendAvatarUrl", source = "friend.avatarUrl")
    @Mapping(target = "status", source = "status")
    UserContactResponseDto toResponse(UserContactEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "friend", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastInteractionAt", ignore = true)
    UserContactEntity toEntity(UserContactRequestDto request);
}