package com.hostel.management.repository;

import com.hostel.management.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findAllByOrderByCreatedAtDesc();
}
