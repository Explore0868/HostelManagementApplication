package com.hostel.management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hostels", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hostels_public_id", columnNames = "public_id"),
        @UniqueConstraint(name = "uk_hostels_owner", columnNames = "owner_user_id")
})
public class Hostel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "contact", length = 40)
    private String contact;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Hostel() {}

    public Hostel(String name, String address, String contact, User owner) {
        this.publicId = UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.contact = contact;
        this.owner = owner;
    }

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public String getContact() { return contact; }
    public User getOwner() { return owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
