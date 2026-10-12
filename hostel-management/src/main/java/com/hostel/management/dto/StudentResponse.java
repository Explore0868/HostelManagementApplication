package com.hostel.management.dto;

import java.time.LocalDate;

public record StudentResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String studentCode,
        String phone,
        String course,
        Integer studyYear,
        String guardianName,
        String guardianPhone,
        LocalDate admissionDate,
        boolean active,
        String hostelName
) {
    public StudentResponse(
            Long id,
            Long userId,
            String fullName,
            String email,
            String studentCode,
            String phone,
            String course,
            Integer studyYear,
            String guardianName,
            String guardianPhone,
            LocalDate admissionDate,
            boolean active) {
        this(id, userId, fullName, email, studentCode, phone, course, studyYear,
             guardianName, guardianPhone, admissionDate, active, null);
    }
}
