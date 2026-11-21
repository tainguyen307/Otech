package com.vn.otech.notification.controller;

import com.vn.otech.notification.dto.NotificationPageResponse;
import com.vn.otech.notification.dto.NotificationUnreadCountResponse;
import com.vn.otech.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPageResponse list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @Size(max = 128) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return notificationService.list(principal.getUsername(), cursor, limit);
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse unreadCount(@AuthenticationPrincipal UserDetails principal) {
        return notificationService.unreadCount(principal.getUsername());
    }

    @PatchMapping("/{notificationId}/read")
    public void markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID notificationId) {
        notificationService.markRead(principal.getUsername(), notificationId);
    }

    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal UserDetails principal) {
        notificationService.markAllRead(principal.getUsername());
    }
}
