package ru.messenger.user_service.contact_service;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestDto {
    private String type;      // "PHONE", "EMAIL", "TELEGRAM"
    private String value;     // "+123456789", "user@mail.com"
    private boolean isPrimary;

    // Валидация (опционально)
    @AssertTrue(message = "Invalid phone format")
    public boolean isValidPhone() {
        if ("PHONE".equals(type)) {
            return value != null && value.matches("^\\+?[0-9\\s\\-()]{7,20}$");
        }
        return true;
    }
}
