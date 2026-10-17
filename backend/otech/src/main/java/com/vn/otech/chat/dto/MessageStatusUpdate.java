package com.vn.otech.chat.dto;

import com.vn.otech.entity.MessageStatus;
import java.util.UUID;

public record MessageStatusUpdate(UUID conversationId, UUID readerId, MessageStatus status) {
}
