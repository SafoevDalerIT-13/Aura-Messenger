package ru.messenger.group_service.domain.entity.enums;

public enum GroupType {
    GROUP,      // Группа (чаты, посты, участники)
    CHANNEL,    // Канал (только посты, без чатов)
    COMMUNITY   // Сообщество (с подгруппами)
}