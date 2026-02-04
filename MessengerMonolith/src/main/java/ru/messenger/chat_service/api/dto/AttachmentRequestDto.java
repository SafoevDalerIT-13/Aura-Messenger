package ru.messenger.chat_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentRequestDto {

    @NotBlank(message = "Имя файла обязательно")
    @Size(max = 255, message = "Имя файла не должно превышать 255 символов")
    private String fileName;

    @NotBlank(message = "URL файла обязателен")
    @Size(max = 500, message = "URL файла не должен превышать 500 символов")
    private String fileUrl;

    @Size(max = 100, message = "Тип файла не должен превышать 100 символов")
    private String fileType;

    @NotNull(message = "Размер файла обязателен")
    @Positive(message = "Размер файла должен быть положительным")
    private Long fileSize;
}