package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginatedResponseDto<T> {

    private int page;

    private int size;

    private int totalPages;

    private long totalElements;

    private boolean hasNext;

    private boolean hasPrevious;

    private T content;
}