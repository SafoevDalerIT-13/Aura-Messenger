package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatUpdateRequestDto {

    @Size(max = 100, message = "Название чата не должно превышать 100 символов")
    private String name;

    @Size(max = 500, message = "URL аватара не должен превышать 500 символов")
    private String avatarUrl;
}