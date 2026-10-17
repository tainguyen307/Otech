package com.vn.otech.chat.dto;

import com.vn.otech.entity.MessageStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ParticipantResponse participant,
        ProductPreview product,
        MessagePreview lastMessage,
        long unreadCount,
        LocalDateTime createdAt) {
    public record ParticipantResponse(UUID id, String fullName, String avatarUrl) {
    }

    public record ProductPreview(UUID id, String title, String imageUrl) {
    }

    public record MessagePreview(String content, UUID senderId, MessageStatus status, LocalDateTime createdAt) {
    }
}
