package ru.messenger.chat_service.domain.entity.enums;

public enum ChatType {
    PRIVATE, // Личный чат (2 участника)
    GROUP,   // Групповой чат
    CHANNEL  // Канал (только админ пишет)
}
