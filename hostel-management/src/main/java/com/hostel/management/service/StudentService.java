package com.hostel.management.service;

import com.hostel.management.dto.StudentRequest;
import com.hostel.management.dto.StudentUpdateRequest;
import com.hostel.management.entity.RoomAllocation;
import com.hostel.management.repository.RoomAllocationRepository;
import com.hostel.management.dto.StudentResponse;
import com.hostel.management.entity.Student;
import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.StudentRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.repository.HostelRepository;
import com.hostel.management.repository.AttendanceRepository;
import com.hostel.management.entity.FeeRecord;
import com.hostel.management.entity.Payment;
import com.hostel.management.repository.FeeRecordRepository;
import com.hostel.management.repository.PaymentRepository;
import com.hostel.management.repository.AnnouncementReactionRepository;
import com.hostel.management.security.SecurityUtils;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomAllocationRepository allocationRepository;
    private final SecurityUtils securityUtils;
    private final HostelRepository hostelRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final AnnouncementReactionRepository announcementReactionRepository;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoomAllocationRepository allocationRepository,
            SecurityUtils securityUtils,
            HostelRepository hostelRepository,
            AttendanceRepository attendanceRepository,
            FeeRecordRepository feeRecordRepository,
            PaymentRepository paymentRepository,
            AnnouncementReactionRepository announcementReactionRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.allocationRepository = allocationRepository;
        this.securityUtils = securityUtils;
        this.hostelRepository = hostelRepository;
        this.attendanceRepository = attendanceRepository;
        this.feeRecordRepository = feeRecordRepository;
        this.paymentRepository = paymentRepository;
        this.announcementReactionRepository = announcementReactionRepository;
    }

    public String getStudentHostel(Student student) {
        if (student == null) return null;
        if (student.getHostelName() != null && !student.getHostelName().isBlank()) {
            return student.getHostelName();
        }
        if (student.getUser() != null && student.getUser().getHostelName() != null
                && !student.getUser().getHostelName().isBlank()) {
            return student.getUser().getHostelName();
        }
        return allocationRepository.findByStudent_IdAndActiveTrue(student.getId())
                .map(a -> a.getRoom().getEffectiveHostelName())
                .orElse(null);
    }

    @Transactional
    public StudentResponse registerStudent(StudentRequest request) {
        String adminHostel = securityUtils.getCurrentUserHostel();
        String targetHostel;
        if (securityUtils.isSuperAdmin()) {
            targetHostel = request.hostelName() == null ? "" : request.hostelName().trim();
            if (!targetHostel.isBlank() && !hostelRepository.existsByNameIgnoreCase(targetHostel)) {
                throw new IllegalArgumentException("Select a hostel registered in this system.");
            }
        } else {
            targetHostel = adminHostel;
        }

        if (targetHostel == null || targetHostel.isBlank()) {
            throw new IllegalArgumentException(
                    "Hostel name is required when a system administrator registers a student.");
        }
        securityUtils.enforceHostelAccess(targetHostel);

        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new IllegalStateException(
                    "An account with this email already exists");
        }

        if (studentCodeExistsInHostel(
                request.studentCode().trim(), targetHostel, null)) {
            throw new IllegalStateException(
                    "This student code is already registered in this hostel");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.STUDENT);
        user.setEnabled(true);
        user.setHostelName(targetHostel);

        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setStudentCode(request.studentCode().trim());
        student.setPhone(request.phone().trim());
        student.setCourse(request.course().trim());
        student.setStudyYear(request.studyYear());
        student.setGuardianName(request.guardianName().trim());
        student.setGuardianPhone(request.guardianPhone().trim());
        student.setAdmissionDate(
                request.admissionDate() != null
                        ? request.admissionDate()
                        : LocalDate.now());
        student.setActive(true);
        student.setHostelName(targetHostel);
        student.setUser(savedUser);

        Student savedStudent = studentRepository.save(student);

        return toResponse(savedStudent);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        if (securityUtils.isSuperAdmin()) {
            return studentRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }
        return studentRepository.findAll()
                .stream()
                .filter(s -> securityUtils.canAccessHostel(getStudentHostel(s)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + id));

        securityUtils.enforceHostelAccess(getStudentHostel(student));
        return toResponse(student);
    }

    public StudentResponse toResponse(Student student) {
        User user = student.getUser();

        return new StudentResponse(
                student.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                student.getStudentCode(),
                student.getPhone(),
                student.getCourse(),
                student.getStudyYear(),
                student.getGuardianName(),
                student.getGuardianPhone(),
                student.getAdmissionDate(),
                student.isActive(),
                getStudentHostel(student)
        );
    }

    @Transactional
    public StudentResponse updateStudent(
            Long id, StudentUpdateRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + id));

        securityUtils.enforceHostelAccess(getStudentHostel(student));

        User user = student.getUser();

        String email = request.email().trim();
        String studentCode = request.studentCode().trim();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(
                email, user.getId())) {
            throw new IllegalStateException(
                    "An account with this email already exists");
        }

        if (studentCodeExistsInHostel(
                studentCode, getStudentHostel(student), id)) {
            throw new IllegalStateException(
                    "This student code is already registered in this hostel");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);

        student.setStudentCode(studentCode);
        student.setPhone(request.phone().trim());
        student.setCourse(request.course().trim());
        student.setStudyYear(request.studyYear());
        student.setGuardianName(request.guardianName().trim());
        student.setGuardianPhone(request.guardianPhone().trim());

        if (request.admissionDate() != null) {
            student.setAdmissionDate(request.admissionDate());
        }

        userRepository.save(user);
        Student saved = studentRepository.save(student);

        return toResponse(saved);
    }

    @Transactional
    public StudentResponse deactivateStudent(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + id));

        securityUtils.enforceHostelAccess(getStudentHostel(student));

        if (!student.isActive()) {
            throw new IllegalStateException(
                    "Student is already inactive.");
        }

        if (allocationRepository
                .existsByStudent_IdAndActiveTrue(id)) {
            throw new IllegalStateException(
                    "Vacate the student's active room allocation before deactivation.");
        }

        student.setActive(false);
        student.getUser().setEnabled(false);

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse reactivateStudent(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + id));

        securityUtils.enforceHostelAccess(getStudentHostel(student));

        if (student.isActive()) {
            throw new IllegalStateException(
                    "Student is already active.");
        }

        student.setActive(true);
        student.getUser().setEnabled(true);

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + studentId
                ));

        securityUtils.enforceHostelAccess(getStudentHostel(student));

        List<FeeRecord> feeRecords = feeRecordRepository.findByStudent_IdOrderByDueDateDesc(studentId);
        boolean hasDuePayment = feeRecords.stream().anyMatch(fee -> {
            if (fee.getStatus() != FeeRecord.FeeStatus.PAID) {
                return true;
            }
            BigDecimal total = fee.getTotalAmount() != null ? fee.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
            return total.compareTo(paid) > 0;
        });

        if (hasDuePayment) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete this student because they have pending or due fee payments. Clear all dues before deleting."
            );
        }

        User linkedUser = student.getUser();

        try {
            for (FeeRecord fee : feeRecords) {
                List<Payment> payments = paymentRepository.findByFeeRecord_IdOrderByPaymentDateDesc(fee.getId());
                if (!payments.isEmpty()) {
                    paymentRepository.deleteAll(payments);
                }
            }
            paymentRepository.flush();

            if (!feeRecords.isEmpty()) {
                feeRecordRepository.deleteAll(feeRecords);
                feeRecordRepository.flush();
            }

            allocationRepository.deleteByStudent_Id(studentId);
            allocationRepository.flush();

            attendanceRepository.deleteByStudent_Id(studentId);
            attendanceRepository.flush();

            announcementReactionRepository.deleteByStudent_Id(studentId);
            announcementReactionRepository.flush();

            studentRepository.delete(student);
            studentRepository.flush();

            if (linkedUser != null) {
                userRepository.delete(linkedUser);
                userRepository.flush();
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot permanently delete this student because dependent "
                            + "records exist. Deactivate the student instead.",
                    exception
            );
        }
    }

    private boolean studentCodeExistsInHostel(
            String studentCode, String hostelName, Long excludedStudentId) {
        String requestedHostel = SecurityUtils.normalizeHostel(hostelName);
        return studentRepository.findAllByStudentCodeIgnoreCase(studentCode).stream()
                .filter(existing -> excludedStudentId == null
                        || !excludedStudentId.equals(existing.getId()))
                .anyMatch(existing -> requestedHostel.equals(
                        SecurityUtils.normalizeHostel(getStudentHostel(existing))));
    }
}
