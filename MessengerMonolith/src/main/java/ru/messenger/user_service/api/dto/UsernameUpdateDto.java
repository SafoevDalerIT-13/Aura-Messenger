package ru.messenger.user_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsernameUpdateDto {
    @Size(min = 3,max = 50)
    @NotBlank(message = "Имя пользователя обязательно")
    private String username;
}
