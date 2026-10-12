package com.hostel.management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "owner_notifications")
public class OwnerNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "hostel_name", nullable = false, length = 100)
    private String hostelName;
    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected OwnerNotification() {}
    public OwnerNotification(Long userId, String hostelName, String eventType, String message) {
        this.userId = userId; this.hostelName = hostelName; this.eventType = eventType; this.message = message;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getHostelName() { return hostelName; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
