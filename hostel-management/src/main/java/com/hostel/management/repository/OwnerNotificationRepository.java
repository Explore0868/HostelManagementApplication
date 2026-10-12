package com.hostel.management.repository;

import com.hostel.management.entity.OwnerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OwnerNotificationRepository extends JpaRepository<OwnerNotification, Long> {
    List<OwnerNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<OwnerNotification> findByIdAndUserId(Long id, Long userId);
}
