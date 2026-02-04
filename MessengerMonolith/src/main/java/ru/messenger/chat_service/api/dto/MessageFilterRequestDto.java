package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.messenger.chat_service.domain.entity.enums.MessageType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageFilterRequestDto {

    @Min(value = 0, message = "Номер страницы должен быть не меньше 0")
    private int page = 0;

    @Min(value = 1, message = "Размер страницы должен быть не меньше 1")
    private int size = 50;

    @NotNull(message = "ID чата обязательно")
    private Long chatId;

    private Long senderId;

    private MessageType messageType;
}