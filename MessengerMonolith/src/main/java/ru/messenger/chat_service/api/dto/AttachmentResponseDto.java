package ru.messenger.chat_service.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentResponseDto {

    private Long id;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private Long messageId;
}