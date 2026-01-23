package ru.messenger.user_service.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DescriptionUpdateDto {
    @Size(max = 500)
    private String description;
}