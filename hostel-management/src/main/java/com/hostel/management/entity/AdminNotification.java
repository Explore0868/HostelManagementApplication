package com.hostel.management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications")
public class AdminNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;
    @Column(name = "owner_user_id")
    private Long ownerUserId;
    @Column(name = "owner_name", length = 100)
    private String ownerName;
    @Column(name = "hostel_public_id", length = 36)
    private String hostelPublicId;
    @Column(name = "hostel_name", length = 100)
    private String hostelName;
    @Column(name = "previous_hostel_name", length = 100)
    private String previousHostelName;
    @Column(name = "new_hostel_name", length = 100)
    private String newHostelName;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected AdminNotification() {}
    public AdminNotification(User owner, Hostel hostel) {
        this.eventType = "OWNER_PASSWORD_CHANGED";
        this.ownerUserId = owner.getId();
        this.ownerName = owner.getFullName();
        this.hostelPublicId = hostel == null ? null : hostel.getPublicId();
        this.hostelName = hostel == null ? owner.getHostelName() : hostel.getName();
    }
    public AdminNotification(Hostel hostel, String previousName, String newName) {
        this.eventType = "HOSTEL_NAME_CHANGED";
        this.ownerUserId = hostel.getOwner().getId();
        this.ownerName = hostel.getOwner().getFullName();
        this.hostelPublicId = hostel.getPublicId();
        this.hostelName = newName;
        this.previousHostelName = previousName;
        this.newHostelName = newName;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public Long getOwnerUserId() { return ownerUserId; }
    public String getOwnerName() { return ownerName; }
    public String getHostelPublicId() { return hostelPublicId; }
    public String getHostelName() { return hostelName; }
    public String getPreviousHostelName() { return previousHostelName; }
    public String getNewHostelName() { return newHostelName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
