package com.hostel.management.service;

import com.hostel.management.entity.Room;
import com.hostel.management.dto.RoomAllocationRequest;
import com.hostel.management.dto.RoomAllocationResponse;
import com.hostel.management.entity.RoomAllocation;
import com.hostel.management.entity.Student;
import com.hostel.management.repository.RoomAllocationRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.StudentRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomAllocationService {

    private final RoomAllocationRepository allocationRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final SecurityUtils securityUtils;
    private final StudentService studentService;

    public RoomAllocationService(
            RoomAllocationRepository allocationRepository,
            RoomRepository roomRepository,
            StudentRepository studentRepository,
            SecurityUtils securityUtils,
            StudentService studentService) {
        this.allocationRepository = allocationRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.securityUtils = securityUtils;
        this.studentService = studentService;
    }

    public List<RoomAllocation> getAllActiveAllocations() {
        if (securityUtils.isSuperAdmin()) {
            return allocationRepository.findByActiveTrue();
        }
        return allocationRepository.findByActiveTrue().stream()
                .filter(this::canAccessAllocation)
                .toList();
    }

    public RoomAllocation getAllocationById(Long allocationId) {
        if (allocationId == null || allocationId <= 0) {
            throw new IllegalArgumentException("Invalid allocation ID.");
        }

        RoomAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Allocation not found: " + allocationId));

        enforceAllocationAccess(allocation);
        return allocation;
    }

    public List<RoomAllocation> getRoomAllocations(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("Invalid room ID.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());

        return allocationRepository.findByRoom_IdAndActiveTrue(roomId).stream()
                .filter(this::canAccessAllocation)
                .toList();
    }

    public RoomAllocation getStudentActiveAllocation(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID.");
        }

        RoomAllocation allocation = allocationRepository
                .findByStudent_IdAndActiveTrue(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student has no active room allocation."));

        enforceAllocationAccess(allocation);
        return allocation;
    }

    @Transactional
    public RoomAllocation allocateRoom(
            Long studentId,
            Long roomId,
            Integer bedNumber) {

        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("Valid student ID is required.");
        }

        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("Valid room ID is required.");
        }

        if (bedNumber == null || bedNumber < 1) {
            throw new IllegalArgumentException(
                    "Bed number must be at least 1.");
        }

        // Lock the room to serialize competing allocation requests.
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Room not found: " + roomId));

        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());

        // Lock the student to prevent simultaneous assignments.
        Student student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + studentId));

        String studentHostel = studentService.getStudentHostel(student);
        securityUtils.enforceHostelAccess(studentHostel);

        if (!SecurityUtils.normalizeHostel(studentHostel)
                .equalsIgnoreCase(SecurityUtils.normalizeHostel(room.getEffectiveHostelName()))) {
            throw new IllegalStateException(
                    "A student can only be allocated to a room in their assigned hostel.");
        }

        if (!room.isActive()) {
            throw new IllegalStateException(
                    "Cannot allocate a student to an inactive room.");
        }

        if (!student.isActive()) {
            throw new IllegalStateException(
                    "Cannot allocate an inactive student.");
        }

        if (bedNumber > room.getCapacity()) {
            throw new IllegalArgumentException(
                    "Bed number cannot exceed the room capacity.");
        }

        if (allocationRepository.existsByStudent_IdAndActiveTrue(studentId)) {
            throw new IllegalStateException(
                    "Student already has an active room allocation.");
        }

        if (allocationRepository
                .existsByRoom_IdAndBedNumberAndActiveTrue(
                        roomId, bedNumber)) {
            throw new IllegalStateException(
                    "This bed is already occupied.");
        }

        long occupiedBeds =
                allocationRepository.countByRoom_IdAndActiveTrue(roomId);

        if (occupiedBeds >= room.getCapacity()) {
            throw new IllegalStateException(
                    "Room is full. No beds are available.");
        }

        // Keep student's hostel synchronized with their allocated room
        if (student.getHostelName() == null || student.getHostelName().isBlank()) {
            student.setHostelName(room.getEffectiveHostelName());
            studentRepository.save(student);
        }

        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setBedNumber(bedNumber);
        allocation.setActive(true);
        allocation.setVacatedDate(null);

        return allocationRepository.save(allocation);
    }

    @Transactional
    public RoomAllocation vacateRoom(Long allocationId) {
        RoomAllocation allocation = getAllocationById(allocationId);
        enforceAllocationAccess(allocation);

        if (!allocation.isActive()) {
            throw new IllegalStateException(
                    "This allocation has already been vacated.");
        }

        allocation.setActive(false);
        allocation.setVacatedDate(LocalDate.now());

        return allocationRepository.save(allocation);
    }

    public long getTotalActiveAllocations() {
        return getAllActiveAllocations().size();
    }

    public long getOccupiedBeds(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("Invalid room ID.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomId));
        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());

        return allocationRepository
                .countByRoom_IdAndActiveTrue(roomId);
    }

    @Transactional(readOnly = true)
    public List<RoomAllocationResponse> getAllActiveAllocationResponses() {
        return getAllActiveAllocations()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomAllocationResponse getAllocationResponseById(Long allocationId) {
        return toResponse(getAllocationById(allocationId));
    }

    @Transactional(readOnly = true)
    public List<RoomAllocationResponse> getRoomAllocationResponses(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        securityUtils.enforceHostelAccess(room.getEffectiveHostelName());

        return getRoomAllocations(roomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoomAllocationResponse allocateRoomResponse(
            Long studentId, Long roomId, Integer bedNumber) {
        RoomAllocation allocation = allocateRoom(studentId, roomId, bedNumber);
        return toResponse(allocation);
    }

    @Transactional
    public RoomAllocationResponse vacateRoomResponse(Long allocationId) {
        RoomAllocation allocation = vacateRoom(allocationId);
        return toResponse(allocation);
    }

    private RoomAllocationResponse toResponse(RoomAllocation allocation) {
        var student = allocation.getStudent();
        var room = allocation.getRoom();

        return new RoomAllocationResponse(
                allocation.getId(),
                student.getId(),
                student.getStudentCode(),
                student.getUser().getFullName(),
                room.getId(),
                room.getRoomNumber(),
                allocation.getBedNumber(),
                allocation.getAllocationDate(),
                allocation.getVacatedDate(),
                allocation.isActive()
        );
    }

    private boolean canAccessAllocation(RoomAllocation allocation) {
        return securityUtils.canAccessHostel(
                    allocation.getRoom().getEffectiveHostelName())
                && securityUtils.canAccessHostel(
                    studentService.getStudentHostel(allocation.getStudent()));
    }

    private void enforceAllocationAccess(RoomAllocation allocation) {
        String roomHostel = allocation.getRoom().getEffectiveHostelName();
        String studentHostel = studentService.getStudentHostel(allocation.getStudent());
        securityUtils.enforceHostelAccess(roomHostel);
        securityUtils.enforceHostelAccess(studentHostel);
        if (!securityUtils.isSuperAdmin()
                && !SecurityUtils.normalizeHostel(roomHostel).equals(
                    SecurityUtils.normalizeHostel(studentHostel))) {
            throw new IllegalStateException(
                    "The allocation links a student and room from different hostels. Contact a system administrator.");
        }
    }
}
