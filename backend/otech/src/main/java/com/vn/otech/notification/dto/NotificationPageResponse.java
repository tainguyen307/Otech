package com.vn.otech.notification.dto;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> items,
        boolean hasMore,
        String nextCursor,
        long unreadCount) {
}
