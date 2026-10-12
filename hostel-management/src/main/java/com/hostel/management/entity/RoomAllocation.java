package com.hostel.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

@Entity
@Table(name = "room_allocations")
public class RoomAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "student_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_allocation_student")
    )
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "room_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_allocation_room")
    )
    @JsonIgnore
    private Room room;

    @Min(value = 1, message = "Bed number must be at least 1")
    @Column(name = "bed_number", nullable = false)
    private Integer bedNumber;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "vacated_date")
    private LocalDate vacatedDate;

    @Column(nullable = false)
    private boolean active = true;

    public RoomAllocation() {
        // Required by JPA
    }

    @PrePersist
    protected void onCreate() {
        if (allocationDate == null) {
            allocationDate = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Integer getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(Integer bedNumber) {
        this.bedNumber = bedNumber;
    }

    public LocalDate getAllocationDate() {
        return allocationDate;
    }

    public void setAllocationDate(LocalDate allocationDate) {
        this.allocationDate = allocationDate;
    }

    public LocalDate getVacatedDate() {
        return vacatedDate;
    }

    public void setVacatedDate(LocalDate vacatedDate) {
        this.vacatedDate = vacatedDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}