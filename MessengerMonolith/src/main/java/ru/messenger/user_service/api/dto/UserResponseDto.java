package ru.messenger.user_service.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {

    private Long id;

    @Size(min = 3,max = 50,message = "Логин должен быть от 3 до 50 символов")
    @NotBlank(message = "Логин пользователя обязательно")
    private String login;

    @Size(min = 3,max = 50)
    @NotBlank(message = "Имя пользователя обязательно")
    private String username;

    @Email
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Некорректный формат email"
    )
    private String email;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 20)
    private String phoneNumber;

}
