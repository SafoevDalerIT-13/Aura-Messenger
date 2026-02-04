package ru.messenger.group_service.domain.entity.enums;

public enum GroupMemberRole {
    OWNER,      // Владелец
    ADMIN,      // Администратор
    MODERATOR,  // Модератор
    MEMBER,     // Участник
    VIEWER      // Наблюдатель (только чтение)
}