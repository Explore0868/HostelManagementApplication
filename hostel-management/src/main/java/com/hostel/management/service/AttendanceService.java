package com.hostel.management.service;

import com.hostel.management.entity.Attendance;
import com.hostel.management.entity.Student;
import com.hostel.management.dto.AttendanceResponse;
import com.hostel.management.repository.AttendanceRepository;
import com.hostel.management.repository.StudentRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final SecurityUtils securityUtils;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            StudentRepository studentRepository,
            StudentService studentService,
            SecurityUtils securityUtils) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.studentService = studentService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendance() {
        if (securityUtils.isSuperAdmin()) {
            return attendanceRepository.findAllByOrderByAttendanceDateDesc().stream()
                    .map(this::toResponse).toList();
        }
        return attendanceRepository.findAllByOrderByAttendanceDateDesc().stream()
                .filter(a -> securityUtils.canAccessHostel(studentService.getStudentHostel(a.getStudent())))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        if (securityUtils.isSuperAdmin()) {
            return attendanceRepository.findByAttendanceDateOrderByIdDesc(date).stream()
                    .map(this::toResponse).toList();
        }
        return attendanceRepository.findByAttendanceDateOrderByIdDesc(date).stream()
                .filter(a -> securityUtils.canAccessHostel(studentService.getStudentHostel(a.getStudent())))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found: " + studentId));

        securityUtils.enforceStudentSelfAccess(student.getUser().getId());

        // If admin is viewing, enforce hostel isolation; students can view their own
        if (!securityUtils.getCurrentUser()
                .map(u -> u.getId().equals(student.getUser().getId()))
                .orElse(false)) {
            securityUtils.enforceHostelAccess(studentService.getStudentHostel(student));
        }

        return attendanceRepository
                .findByStudent_IdOrderByAttendanceDateDesc(studentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AttendanceResponse markAttendance(
            Long studentId,
            LocalDate date,
            Attendance.AttendanceStatus status) {

        if (date == null || status == null) {
            throw new IllegalArgumentException(
                    "Attendance date and status are required");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + studentId));

        securityUtils.enforceHostelAccess(studentService.getStudentHostel(student));

        Attendance attendance = attendanceRepository
                .findByStudent_IdAndAttendanceDate(studentId, date)
                .orElseGet(Attendance::new);

        attendance.setStudent(student);
        attendance.setAttendanceDate(date);
        attendance.setStatus(status);

        return toResponse(attendanceRepository.save(attendance));
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        Student student = attendance.getStudent();
        return new AttendanceResponse(
                attendance.getId(), student.getId(), student.getStudentCode(),
                student.getUser().getFullName(), attendance.getAttendanceDate(),
                attendance.getStatus());
    }
}
