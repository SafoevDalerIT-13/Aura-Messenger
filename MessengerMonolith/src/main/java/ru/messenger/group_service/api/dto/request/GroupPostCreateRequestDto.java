package ru.messenger.group_service.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.messenger.group_service.domain.entity.enums.GroupPostType;

@Data
public class GroupPostCreateRequestDto {
    @NotNull(message = "ID группы обязателен")
    private Long groupId;

    @NotBlank(message = "Содержание поста обязательно")
    @Size(max = 2000, message = "Пост не должен превышать 2000 символов")
    private String content;

    private GroupPostType type = GroupPostType.POST;
}