package com.vn.otech.chat.dto;

import java.util.UUID;

public record ConversationCreateRequest(UUID productId, UUID recipientId) {
}
