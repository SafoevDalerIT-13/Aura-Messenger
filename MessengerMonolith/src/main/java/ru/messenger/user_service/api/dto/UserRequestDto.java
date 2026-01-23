package ru.messenger.user_service.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRequestDto {

    @Size(min = 3,max = 50)
    @NotBlank(message = "Имя пользователя обязательно")
    private String username;

    @Email
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Некорректный формат email"
    )
    private String email;

    @Size(min = 6,max = 100)
    @NotBlank(message = "Пароль обязателен")
    private String password;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;


}
