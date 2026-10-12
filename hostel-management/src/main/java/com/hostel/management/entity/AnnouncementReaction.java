package com.hostel.management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "announcement_reactions", uniqueConstraints = @UniqueConstraint(
        name = "uk_announcement_reaction_student",
        columnNames = {"announcement_id", "student_id"}))
public class AnnouncementReaction {

    public enum Reaction { LIKE, DISLIKE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Reaction reaction;

    public Long getId() { return id; }
    public Announcement getAnnouncement() { return announcement; }
    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Reaction getReaction() { return reaction; }
    public void setReaction(Reaction reaction) { this.reaction = reaction; }
}
