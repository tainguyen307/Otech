package com.vn.otech.chat.dto;

import com.vn.otech.entity.MessageStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID clientMessageId,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        MessageStatus status,
        LocalDateTime createdAt) {
}
