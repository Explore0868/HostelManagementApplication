package com.hostel.management.dto;

import com.hostel.management.entity.Attendance;
import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        LocalDate attendanceDate,
        Attendance.AttendanceStatus status
) {}
