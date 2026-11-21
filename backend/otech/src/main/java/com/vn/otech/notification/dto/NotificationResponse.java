package com.vn.otech.notification.dto;

import com.vn.otech.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String content,
        UUID referenceId,
        boolean read,
        LocalDateTime createdAt) {
}
