package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.NotificationUserState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationUserStateRepository extends JpaRepository<NotificationUserState, Long> {
    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    List<NotificationUserState> findAllByNotificationId(Long notificationId);

    Optional<NotificationUserState> findByNotificationIdAndUserId(Long notificationId, Long userId);

    @Query("""
        SELECT state
        FROM NotificationUserState state
        JOIN FETCH state.notification notification
        LEFT JOIN FETCH notification.createdBy
        WHERE state.user.id = :userId
          AND notification.resolvedAt IS NULL
          AND (notification.expiresAt IS NULL OR notification.expiresAt > :now)
          AND (:includeDismissed = true OR state.dismissedAt IS NULL)
    """)
    List<NotificationUserState> findVisibleForUser(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        @Param("includeDismissed") boolean includeDismissed
    );

    @Query("""
        SELECT COUNT(state)
        FROM NotificationUserState state
        JOIN state.notification notification
        WHERE state.user.id = :userId
          AND state.readAt IS NULL
          AND state.dismissedAt IS NULL
          AND notification.resolvedAt IS NULL
          AND (notification.expiresAt IS NULL OR notification.expiresAt > :now)
    """)
    long countUnreadForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
