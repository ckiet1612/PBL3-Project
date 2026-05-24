package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Notification;
import com.pbl3.project.pbl3_project.entity.NotificationCategory;
import com.pbl3.project.pbl3_project.entity.NotificationType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventKey(String eventKey);

    List<Notification> findAllByCategoryAndTypeInAndResolvedAtIsNull(
        NotificationCategory category,
        Collection<NotificationType> types
    );
}
