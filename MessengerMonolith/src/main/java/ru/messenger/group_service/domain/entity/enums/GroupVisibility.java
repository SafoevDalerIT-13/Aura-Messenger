package ru.messenger.group_service.domain.entity.enums;

public enum GroupVisibility {
    PUBLIC,     // Виден всем, можно присоединиться
    PRIVATE,    // Только по приглашению
    HIDDEN      // Скрытый, только по ссылке
}