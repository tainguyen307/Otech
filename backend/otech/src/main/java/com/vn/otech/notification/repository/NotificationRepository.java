package com.vn.otech.notification.repository;

import com.vn.otech.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUser_IdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    @Query("""
            select notification from Notification notification
            where notification.user.id = :userId
              and (notification.createdAt < :createdAt
                   or (notification.createdAt = :createdAt and notification.id < :id))
            order by notification.createdAt desc, notification.id desc
            """)
    List<Notification> findPageAfter(
            @Param("userId") UUID userId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") UUID id,
            Pageable pageable);

    Optional<Notification> findByIdAndUser_Id(UUID id, UUID userId);

    long countByUser_IdAndReadFalse(UUID userId);

    @Modifying
    @Query("update Notification notification set notification.read = true "
            + "where notification.user.id = :userId and notification.read = false")
    int markAllRead(@Param("userId") UUID userId);
}
