package ru.messenger.user_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvatarResponseDto {
    @NotNull
    private String avatarUrl;

    @NotBlank
    @Size(max = 100)
    private String message;
}
