package ru.messenger.group_service.api.mapper;

import org.mapstruct.*;
import ru.messenger.group_service.api.dto.request.GroupCommentCreateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupCommentResponseDto;
import ru.messenger.group_service.domain.entity.GroupPostCommentEntity;
import ru.messenger.user_service.api.mapper.UserMapper;

import java.util.List;

/**
 * Маппер для преобразования сущностей комментариев в DTO и обратно.
 * Использует MapStruct для автоматической генерации кода.
 */
@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface GroupCommentMapper {

    /**
     * Преобразовать сущность комментария в DTO для ответа
     */
    @Named("toResponseDto")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "authorAvatarUrl", source = "author.avatarUrl")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "likesCount", source = "likesCount")
    @Mapping(target = "repliesCount", expression = "java(calculateRepliesCount(entity))")
    @Mapping(target = "likedByMe", ignore = true) // Заполняется отдельно
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    GroupCommentResponseDto toResponseDto(GroupPostCommentEntity entity);

    /**
     * Преобразовать список сущностей комментариев в список DTO
     */
    @IterableMapping(qualifiedByName = "toResponseDto")
    List<GroupCommentResponseDto> toResponseDtoList(List<GroupPostCommentEntity> entities);

    /**
     * Преобразовать страницу сущностей комментариев в страницу DTO
     */
    default org.springframework.data.domain.Page<GroupCommentResponseDto> toResponseDtoPage(
            org.springframework.data.domain.Page<GroupPostCommentEntity> page) {
        return page.map(this::toResponseDto);
    }

    /**
     * Преобразовать DTO для создания комментария в сущность
     * Игнорируем поля, которые будут установлены отдельно
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "parentComment", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "likesCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GroupPostCommentEntity toEntity(GroupCommentCreateRequestDto dto);

    /**
     * Обновить сущность комментария из DTO обновления
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "parentComment", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "likesCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(@MappingTarget GroupPostCommentEntity entity, GroupCommentCreateRequestDto dto);

    /**
     * Обогатить DTO дополнительной информацией
     * Этот метод используется для добавления вычисляемых полей
     */
    @AfterMapping
    default void enrichDto(@MappingTarget GroupCommentResponseDto dto, GroupPostCommentEntity entity) {
        // Можно добавить дополнительную логику обогащения DTO
        // Например, форматирование дат, вычисление производных полей и т.д.
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Вычислить количество ответов на комментарий
     * Этот метод будет использоваться в expression в @Mapping
     */
    default Integer calculateRepliesCount(GroupPostCommentEntity entity) {
        if (entity == null || entity.getReplies() == null) {
            return 0;
        }
        // Считаем только активные (не удаленные) ответы
        return (int) entity.getReplies().stream()
                .filter(reply -> !"[Комментарий удален]".equals(reply.getContent()))
                .count();
    }

    /**
     * Создать DTO с минимальной информацией (для оптимизации)
     */
    @Named("toMinimalResponseDto")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "likesCount", source = "likesCount")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "postId", ignore = true)
    @Mapping(target = "authorAvatarUrl", ignore = true)
    @Mapping(target = "parentCommentId", ignore = true)
    @Mapping(target = "repliesCount", ignore = true)
    @Mapping(target = "likedByMe", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GroupCommentResponseDto toMinimalResponseDto(GroupPostCommentEntity entity);

    /**
     * Преобразовать список сущностей в список минимальных DTO
     */
    @IterableMapping(qualifiedByName = "toMinimalResponseDto")
    List<GroupCommentResponseDto> toMinimalResponseDtoList(List<GroupPostCommentEntity> entities);

    /**
     * Создать DTO для вложенных комментариев (рекурсивно)
     */
    default GroupCommentResponseDto toNestedResponseDto(GroupPostCommentEntity entity, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        GroupCommentResponseDto dto = toResponseDto(entity);

        // Рекурсивно преобразовываем ответы
        if (entity.getReplies() != null && !entity.getReplies().isEmpty()) {
            List<GroupCommentResponseDto> replies = entity.getReplies().stream()
                    .map(reply -> toNestedResponseDto(reply, currentUserId))
                    .toList();
            // Здесь можно установить replies в dto, если нужно
        }

        return dto;
    }
}