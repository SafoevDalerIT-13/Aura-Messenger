package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;
import ru.messenger.chat_service.domain.entity.enums.ChatType;

@Data
@Builder
public class ChatFilterRequestDto {

    @Min(value = 0, message = "Номер страницы должен быть не меньше 0")
    private int page = 0;

    @Min(value = 1, message = "Размер страницы должен быть не меньше 1")
    private int size = 20;

    private String searchQuery;

    private ChatType chatType;
}