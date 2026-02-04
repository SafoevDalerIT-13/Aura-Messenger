package ru.messenger.user_service.contact_service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponseDto {
    private Long id;
    private String type;
    private String value;
    private boolean isPrimary;
    private Long userId; // или username
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}