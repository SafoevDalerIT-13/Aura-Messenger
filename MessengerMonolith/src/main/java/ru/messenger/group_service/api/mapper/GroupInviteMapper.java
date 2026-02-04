package ru.messenger.group_service.api.mapper;

import org.mapstruct.*;
import ru.messenger.group_service.api.dto.request.GroupInviteRequestDto;
import ru.messenger.group_service.api.dto.response.GroupInviteResponseDto;
import ru.messenger.group_service.domain.entity.GroupInviteEntity;
import ru.messenger.group_service.domain.entity.enums.GroupInviteStatus;
import ru.messenger.user_service.api.mapper.UserMapper;

import java.time.Instant;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface GroupInviteMapper {

    /**
     * Преобразовать сущность приглашения в DTO для ответа
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "inviterId", source = "inviter.id")
    @Mapping(target = "inviterUsername", source = "inviter.username")
    @Mapping(target = "invitedId", source = "invited.id")
    @Mapping(target = "invitedUsername", source = "invited.username")
    @Mapping(target = "invitedEmail", source = "invitedEmail")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "isExpired", expression = "java(isInviteExpired(entity))")
    GroupInviteResponseDto toResponseDto(GroupInviteEntity entity);

    /**
     * Преобразовать список сущностей приглашений в список DTO
     */
    List<GroupInviteResponseDto> toResponseDtoList(List<GroupInviteEntity> entities);

    /**
     * Преобразовать DTO для создания приглашения в сущность
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "inviter", ignore = true)
    @Mapping(target = "invited", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "token", ignore = true) // Генерируется в сервисе
    @Mapping(target = "expiresAt", ignore = true) // Устанавливается в сервисе
    @Mapping(target = "createdAt", ignore = true)
    GroupInviteEntity toEntity(GroupInviteRequestDto dto);

    /**
     * Обновить сущность приглашения из DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "inviter", ignore = true)
    @Mapping(target = "invited", ignore = true)
    @Mapping(target = "invitedEmail", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget GroupInviteEntity entity, GroupInviteRequestDto dto);

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверить, истекло ли приглашение
     */
    default Boolean isInviteExpired(GroupInviteEntity entity) {
        if (entity == null || entity.getExpiresAt() == null) {
            return false;
        }
        return entity.getExpiresAt().isBefore(Instant.now());
    }

    /**
     * Проверить, является ли приглашение действительным
     */
    default Boolean isInviteValid(GroupInviteEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getStatus() == GroupInviteStatus.PENDING && !isInviteExpired(entity);
    }
}