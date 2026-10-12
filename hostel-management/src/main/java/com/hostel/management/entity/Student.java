package com.hostel.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(
    name = "students",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_students_hostel_student_code",
            columnNames = {"hostel_name", "student_code"}
        ),
        @UniqueConstraint(
            name = "uk_students_user_id",
            columnNames = "user_id"
        )
    }
)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Student code is required")
    @Size(max = 30, message = "Student code cannot exceed 30 characters")
    @Column(name = "student_code", nullable = false, length = 30)
    private String studentCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[0-9+() -]{7,20}$",
        message = "Enter a valid phone number"
    )
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank(message = "Course is required")
    @Column(nullable = false, length = 100)
    private String course;

    @Column(name = "study_year", nullable = false)
    private Integer studyYear;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "hostel_name", length = 100)
    private String hostelName;

    @Column(nullable = false)
    private boolean active = true;

    /*
     * Each student profile belongs to exactly one login account.
     * The User entity owns the account's email, password and role.
     */
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_student_user")
    )
    private User user;

    public Student() {
        // Required by JPA
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Integer getStudyYear() {
        return studyYear;
    }

    public void setStudyYear(Integer studyYear) {
        this.studyYear = studyYear;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public void setGuardianPhone(String guardianPhone) {
        this.guardianPhone = guardianPhone;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getHostelName() {
        return hostelName;
    }

    public void setHostelName(String hostelName) {
        this.hostelName = hostelName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
