package ru.messenger.group_service.domain.entity.enums;

public enum GroupInviteStatus {
    PENDING,    // Ожидание
    ACCEPTED,   // Принято
    DECLINED,   // Отклонено
    EXPIRED,    // Истекло
    CANCELLED   // Отменено
}