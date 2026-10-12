package com.hostel.management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hostel_blocks", uniqueConstraints = @UniqueConstraint(
        name = "uk_hostel_blocks_name", columnNames = {"hostel_name", "name"}))
public class HostelBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hostel_name", nullable = false, length = 100)
    private String hostelName;
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected HostelBlock() {}
    public HostelBlock(String hostelName, String name) { this.hostelName = hostelName; this.name = name; }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
