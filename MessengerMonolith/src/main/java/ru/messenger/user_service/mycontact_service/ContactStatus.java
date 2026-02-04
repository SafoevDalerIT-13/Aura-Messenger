package ru.messenger.user_service.mycontact_service;

public enum ContactStatus {
    PENDING,    // Заявка отправлена, ожидает подтверждения
    ACCEPTED,   // Контакт подтвержден (друг)
    REJECTED,   // Заявка отклонена
    BLOCKED     // Пользователь заблокирован
}