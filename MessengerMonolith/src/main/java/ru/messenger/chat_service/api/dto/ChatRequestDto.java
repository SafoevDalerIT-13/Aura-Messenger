package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.messenger.chat_service.domain.entity.enums.ChatType;

import java.util.Set;

@Data
@Builder
public class ChatRequestDto {

    @Size(max = 100, message = "Название чата не должно превышать 100 символов")
    private String name;

    @NotNull(message = "Тип чата обязателен")
    private ChatType type;

    @Size(max = 500, message = "URL аватара не должен превышать 500 символов")
    private String avatarUrl;

    @NotNull(message = "Список участников обязателен")
    @Size(min = 1, message = "Чат должен содержать как минимум одного участника")
    private Set<Long> participantIds;
}