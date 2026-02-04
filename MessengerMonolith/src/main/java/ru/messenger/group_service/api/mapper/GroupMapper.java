package ru.messenger.group_service.api.mapper;

import org.mapstruct.*;
import ru.messenger.group_service.api.dto.request.GroupCreateRequestDto;
import ru.messenger.group_service.api.dto.request.GroupUpdateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupMemberResponseDto;
import ru.messenger.group_service.api.dto.response.GroupPostResponseDto;
import ru.messenger.group_service.api.dto.response.GroupResponseDto;
import ru.messenger.group_service.domain.entity.GroupEntity;
import ru.messenger.group_service.domain.entity.GroupMemberEntity;
import ru.messenger.group_service.domain.entity.GroupPostEntity;
import ru.messenger.user_service.api.mapper.UserMapper;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface GroupMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    GroupResponseDto toResponseDto(GroupEntity entity);

    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    GroupMemberResponseDto toMemberResponseDto(GroupMemberEntity entity);

    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "authorAvatarUrl", source = "author.avatarUrl")
    @Mapping(target = "likedByMe", ignore = true)
    GroupPostResponseDto toPostResponseDto(GroupPostEntity entity);

    List<GroupResponseDto> toResponseDtoList(List<GroupEntity> entities);
    List<GroupMemberResponseDto> toMemberResponseDtoList(List<GroupMemberEntity> entities);
    List<GroupPostResponseDto> toPostResponseDtoList(List<GroupPostEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "invites", ignore = true)
    @Mapping(target = "membersCount", constant = "1")
    @Mapping(target = "postsCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GroupEntity toEntity(GroupCreateRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "visibility", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "invites", ignore = true)
    @Mapping(target = "membersCount", ignore = true)
    @Mapping(target = "postsCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(GroupUpdateRequestDto dto, @MappingTarget GroupEntity entity);
}