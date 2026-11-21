package com.vn.otech.notification.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Notification;
import com.vn.otech.entity.NotificationType;
import com.vn.otech.entity.User;
import com.vn.otech.notification.dto.NotificationPageResponse;
import com.vn.otech.notification.dto.NotificationResponse;
import com.vn.otech.notification.dto.NotificationUnreadCountResponse;
import com.vn.otech.notification.repository.NotificationRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class NotificationService {
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void create(User recipient, NotificationType type, String title, String content, UUID referenceId) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceId(referenceId);
        notificationRepository.save(notification);
    }

    public NotificationPageResponse list(String email, String cursor, int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        User user = findActiveUser(email);
        NotificationCursor decodedCursor = decodeCursor(cursor);
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Notification> results = decodedCursor == null
                ? notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(user.getId(), pageRequest)
                : notificationRepository.findPageAfter(
                        user.getId(), decodedCursor.createdAt(), decodedCursor.id(), pageRequest);
        boolean hasMore = results.size() > limit;
        List<Notification> page = hasMore ? results.subList(0, limit) : results;
        String nextCursor = hasMore && !page.isEmpty() ? encodeCursor(page.getLast()) : null;
        return new NotificationPageResponse(
                page.stream().map(this::toResponse).toList(),
                hasMore,
                nextCursor,
                notificationRepository.countByUser_IdAndReadFalse(user.getId()));
    }

    public NotificationUnreadCountResponse unreadCount(String email) {
        User user = findActiveUser(email);
        return new NotificationUnreadCountResponse(
                notificationRepository.countByUser_IdAndReadFalse(user.getId()));
    }

    @Transactional
    public void markRead(String email, UUID notificationId) {
        User user = findActiveUser(email);
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllRead(String email) {
        User user = findActiveUser(email);
        notificationRepository.markAllRead(user.getId());
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is not active");
        }
        return user;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private NotificationCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid notification cursor");
            }
            return new NotificationCursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification cursor", exception);
        }
    }

    private String encodeCursor(Notification notification) {
        String value = notification.getCreatedAt() + "|" + notification.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record NotificationCursor(LocalDateTime createdAt, UUID id) {
    }
}
