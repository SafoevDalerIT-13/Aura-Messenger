package ru.messenger.group_service.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupUpdateRequestDto {
    @Size(min = 3, max = 100, message = "Название должно быть от 3 до 100 символов")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    private String avatarUrl;
    private String coverUrl;
}