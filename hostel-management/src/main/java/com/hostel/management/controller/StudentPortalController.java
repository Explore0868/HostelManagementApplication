package com.hostel.management.controller;

import com.hostel.management.dto.FeeRecordResponse;
import com.hostel.management.dto.AttendanceResponse;
import com.hostel.management.dto.StudentProfileResponse;
import com.hostel.management.dto.AnnouncementResponse;
import com.hostel.management.entity.Attendance;
import com.hostel.management.entity.Announcement;
import com.hostel.management.entity.AnnouncementReaction;
import com.hostel.management.entity.RoomAllocation;
import com.hostel.management.entity.Student;
import com.hostel.management.entity.User;
import com.hostel.management.repository.AttendanceRepository;
import com.hostel.management.repository.RoomAllocationRepository;
import com.hostel.management.repository.StudentRepository;
import com.hostel.management.repository.AnnouncementRepository;
import com.hostel.management.repository.AnnouncementReactionRepository;
import com.hostel.management.security.SecurityUtils;
import com.hostel.management.service.FeeRecordService;
import com.hostel.management.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentPortalController {

    private final SecurityUtils securityUtils;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final RoomAllocationRepository allocationRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeeRecordService feeRecordService;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReactionRepository reactionRepository;

    public StudentPortalController(
            SecurityUtils securityUtils,
            StudentRepository studentRepository,
            StudentService studentService,
            RoomAllocationRepository allocationRepository,
            AttendanceRepository attendanceRepository,
            FeeRecordService feeRecordService,
            AnnouncementRepository announcementRepository,
            AnnouncementReactionRepository reactionRepository) {
        this.securityUtils = securityUtils;
        this.studentRepository = studentRepository;
        this.studentService = studentService;
        this.allocationRepository = allocationRepository;
        this.attendanceRepository = attendanceRepository;
        this.feeRecordService = feeRecordService;
        this.announcementRepository = announcementRepository;
        this.reactionRepository = reactionRepository;
    }

    private Student getAuthenticatedStudent() {
        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));

        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No student profile associated with this account"));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile() {
        Student student = getAuthenticatedStudent();
        User user = student.getUser();

        String studentHostel = studentService.getStudentHostel(student);
        StudentProfileResponse.AllocatedRoomInfo roomInfo = allocationRepository
                .findByStudent_IdAndActiveTrue(student.getId())
                .filter(a -> SecurityUtils.normalizeHostel(studentHostel)
                        .equals(SecurityUtils.normalizeHostel(a.getRoom().getEffectiveHostelName())))
                .map(a -> new StudentProfileResponse.AllocatedRoomInfo(
                        a.getId(),
                        a.getRoom().getId(),
                        a.getRoom().getRoomNumber(),
                        a.getRoom().getRoomType(),
                        a.getRoom().getBlockName(),
                        a.getBedNumber(),
                        a.getAllocationDate()
                ))
                .orElse(null);

        return new StudentProfileResponse(
                student.getId(),
                user.getId(),
                student.getStudentCode(),
                user.getFullName(),
                user.getEmail(),
                student.getPhone(),
                student.getCourse(),
                student.getStudyYear(),
                student.getGuardianName(),
                student.getGuardianPhone(),
                student.getAdmissionDate(),
                student.isActive(),
                studentService.getStudentHostel(student),
                roomInfo
        );
    }

    @GetMapping("/attendance")
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance() {
        Student student = getAuthenticatedStudent();
        return attendanceRepository.findByStudent_IdOrderByAttendanceDateDesc(student.getId())
                .stream().map(a -> new AttendanceResponse(
                        a.getId(), student.getId(), student.getStudentCode(),
                        student.getUser().getFullName(), a.getAttendanceDate(), a.getStatus()))
                .toList();
    }

    @GetMapping("/announcements")
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getMyAnnouncements() {
        Student student = getAuthenticatedStudent();
        String hostel = studentService.getStudentHostel(student);
        String normalizedHostel = SecurityUtils.normalizeHostel(hostel);
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> a.getHostelName() == null || a.getHostelName().isBlank()
                        || SecurityUtils.normalizeHostel(a.getHostelName()).equals(normalizedHostel))
                .map(a -> {
                    AnnouncementReaction.Reaction mine = reactionRepository
                            .findByAnnouncement_IdAndStudent_Id(a.getId(), student.getId())
                            .map(AnnouncementReaction::getReaction).orElse(null);
                    return AnnouncementResponse.from(a,
                            reactionRepository.countByAnnouncement_IdAndReaction(a.getId(), AnnouncementReaction.Reaction.LIKE),
                            reactionRepository.countByAnnouncement_IdAndReaction(a.getId(), AnnouncementReaction.Reaction.DISLIKE), mine);
                })
                .toList();
    }

    @PatchMapping("/announcements/{id}/reaction")
    @Transactional
    public AnnouncementResponse reactToAnnouncement(@PathVariable Long id, @RequestBody ReactionRequest request) {
        Student student = getAuthenticatedStudent();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        String hostel = SecurityUtils.normalizeHostel(studentService.getStudentHostel(student));
        if (announcement.getHostelName() != null && !announcement.getHostelName().isBlank()
                && !SecurityUtils.normalizeHostel(announcement.getHostelName()).equals(hostel)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This announcement belongs to another hostel");
        }
        AnnouncementReaction.Reaction selected = null;
        if (request != null && request.reaction() != null && !request.reaction().isBlank()) {
            try { selected = AnnouncementReaction.Reaction.valueOf(request.reaction().trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reaction must be LIKE or DISLIKE"); }
        }
        var existing = reactionRepository.findByAnnouncement_IdAndStudent_Id(id, student.getId());
        if (selected == null) {
            existing.ifPresent(reactionRepository::delete);
        } else {
            AnnouncementReaction reaction = existing.orElseGet(AnnouncementReaction::new);
            reaction.setAnnouncement(announcement);
            reaction.setStudent(student);
            reaction.setReaction(selected);
            reactionRepository.save(reaction);
        }
        AnnouncementReaction.Reaction finalSelected = selected;
        return AnnouncementResponse.from(announcement,
                reactionRepository.countByAnnouncement_IdAndReaction(id, AnnouncementReaction.Reaction.LIKE),
                reactionRepository.countByAnnouncement_IdAndReaction(id, AnnouncementReaction.Reaction.DISLIKE), finalSelected);
    }

    public record ReactionRequest(String reaction) {}

    @GetMapping("/fees")
    public List<FeeRecordResponse> getMyFees() {
        Student student = getAuthenticatedStudent();
        return feeRecordService.getFeesByStudent(student.getId());
    }
}
