package ru.messenger.group_service.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupCommentCreateRequestDto {
    @NotBlank(message = "Текст комментария обязателен")
    @Size(max = 1000, message = "Комментарий не должен превышать 1000 символов")
    private String content;

    private Long parentCommentId; // Для вложенных комментариев
}