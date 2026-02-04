package ru.messenger.group_service.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.messenger.group_service.domain.entity.enums.GroupType;
import ru.messenger.group_service.domain.entity.enums.GroupVisibility;

import java.util.Set;

@Data
public class GroupCreateRequestDto {
    @NotBlank(message = "Название группы обязательно")
    @Size(min = 3, max = 100, message = "Название должно быть от 3 до 100 символов")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Тип группы обязателен")
    private GroupType type;

    @NotNull(message = "Видимость группы обязательна")
    private GroupVisibility visibility;

    private String avatarUrl;
    private String coverUrl;
    private Set<Long> initialMembers;
}