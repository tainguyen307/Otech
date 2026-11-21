package com.vn.otech.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Notification;
import com.vn.otech.entity.NotificationType;
import com.vn.otech.entity.User;
import com.vn.otech.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    @Test
    void listsNotificationsWithCursorAndUnreadCount() {
        User user = user("member@example.com");
        Notification first = notification(LocalDateTime.of(2026, 11, 20, 12, 0));
        Notification second = notification(LocalDateTime.of(2026, 11, 20, 11, 0));
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(user.getId(), PageRequest.of(0, 2)))
                .thenReturn(List.of(first, second));
        when(notificationRepository.countByUser_IdAndReadFalse(user.getId())).thenReturn(3L);

        var response = service().list(user.getEmail(), null, 1);

        assertEquals(1, response.items().size());
        assertEquals(first.getId(), response.items().getFirst().id());
        assertEquals(3L, response.unreadCount());
        assertTrue(response.hasMore());
        assertNotNull(response.nextCursor());
    }

    @Test
    void marksOnlyTheCurrentUsersNotificationAsRead() {
        User user = user("member@example.com");
        Notification notification = notification(LocalDateTime.of(2026, 11, 20, 12, 0));
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(notificationRepository.findByIdAndUser_Id(notification.getId(), user.getId()))
                .thenReturn(Optional.of(notification));

        service().markRead(user.getEmail(), notification.getId());

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void marksAllNotificationsReadForTheCurrentUser() {
        User user = user("member@example.com");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        service().markAllRead(user.getEmail());

        verify(notificationRepository).markAllRead(user.getId());
    }

    @Test
    void doesNotAllowReadingAnotherUsersNotification() {
        User user = user("member@example.com");
        UUID notificationId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(notificationRepository.findByIdAndUser_Id(notificationId, user.getId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service().markRead(user.getEmail(), notificationId));
    }

    @Test
    void rejectsMalformedCursorAndInvalidPageSize() {
        User user = user("member@example.com");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class,
                () -> service().list(user.getEmail(), "not-a-cursor", 20));
        assertThrows(ResponseStatusException.class,
                () -> service().list(user.getEmail(), null, 51));
    }

    private NotificationService service() {
        return new NotificationService(notificationRepository, userRepository);
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private Notification notification(LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setType(NotificationType.MESSAGE);
        notification.setTitle("New message");
        notification.setContent("Hello");
        notification.setCreatedAt(createdAt);
        return notification;
    }
}
