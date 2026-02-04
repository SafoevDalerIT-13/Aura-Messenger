package ru.messenger.user_service.mycontact_service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactRequestDto {
    private Long friendId;      // ID пользователя, которого добавляем
    private String nickname;    // Опционально: псевдоним
}
