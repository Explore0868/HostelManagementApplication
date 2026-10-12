
package com.hostel.management.controller;

import com.hostel.management.entity.Attendance;
import com.hostel.management.dto.AttendanceResponse;
import com.hostel.management.service.AttendanceService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public List<AttendanceResponse> getAllAttendance() {
        return attendanceService.getAllAttendance();
    }

    @GetMapping("/date/{date}")
    public List<AttendanceResponse> getAttendanceByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return attendanceService.getAttendanceByDate(date);
    }

    @GetMapping("/student/{studentId}")
    public List<AttendanceResponse> getAttendanceByStudent(
            @PathVariable Long studentId) {
        return attendanceService.getAttendanceByStudent(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public AttendanceResponse markAttendance(@RequestBody AttendanceRequest request) {
        if (request.studentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Student ID is required");
        }

        try {
            return attendanceService.markAttendance(
                    request.studentId(),
                    request.attendanceDate(),
                    request.status());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public record AttendanceRequest(
            Long studentId,
            LocalDate attendanceDate,
            Attendance.AttendanceStatus status) {}
}
