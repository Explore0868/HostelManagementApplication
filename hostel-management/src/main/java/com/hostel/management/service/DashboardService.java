package com.hostel.management.service;

import com.hostel.management.dto.DashboardResponse;
import com.hostel.management.entity.Room;
import com.hostel.management.entity.RoomAllocation;
import com.hostel.management.entity.Student;
import com.hostel.management.repository.StudentRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.RoomAllocationRepository;
import com.hostel.management.security.SecurityUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final RoomAllocationRepository allocationRepository;
    private final StudentService studentService;
    private final SecurityUtils securityUtils;

    public DashboardService(
            StudentRepository studentRepository,
            RoomRepository roomRepository,
            RoomAllocationRepository allocationRepository,
            StudentService studentService,
            SecurityUtils securityUtils) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.allocationRepository = allocationRepository;
        this.studentService = studentService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats() {
        if (!securityUtils.isSuperAdmin()) {
            List<Room> rooms = roomRepository.findAll().stream()
                    .filter(r -> securityUtils.canAccessHostel(r.getEffectiveHostelName()))
                    .toList();

            List<Student> students = studentRepository.findAll().stream()
                    .filter(s -> securityUtils.canAccessHostel(studentService.getStudentHostel(s)))
                    .toList();

            List<RoomAllocation> allocations = allocationRepository.findByActiveTrue().stream()
                    .filter(a -> securityUtils.canAccessHostel(a.getRoom().getEffectiveHostelName())
                            && securityUtils.canAccessHostel(studentService.getStudentHostel(a.getStudent())))
                    .toList();

            long totalStudents = students.size();
            long activeStudents = students.stream().filter(Student::isActive).count();

            long totalRooms = rooms.size();
            long activeRooms = rooms.stream().filter(Room::isActive).count();

            long totalBeds = rooms.stream()
                    .filter(Room::isActive)
                    .mapToLong(Room::getCapacity)
                    .sum();

            long occupiedBeds = allocations.size();
            long availableBeds = Math.max(0L, totalBeds - occupiedBeds);
            long activeAllocations = occupiedBeds;

            double occupancyPercentage = totalBeds == 0
                    ? 0.0
                    : Math.round(((double) occupiedBeds / totalBeds) * 10000.0) / 100.0;

            return new DashboardResponse(
                    totalStudents,
                    activeStudents,
                    totalRooms,
                    activeRooms,
                    totalBeds,
                    occupiedBeds,
                    availableBeds,
                    activeAllocations,
                    occupancyPercentage
            );
        }

        long totalStudents = studentRepository.count();
        long activeStudents = studentRepository.countByActiveTrue();

        long totalRooms = roomRepository.count();
        long activeRooms = roomRepository.countByActiveTrue();

        long totalBeds = roomRepository.sumCapacityOfActiveRooms();
        long occupiedBeds = allocationRepository.countByActiveTrue();

        long availableBeds = Math.max(0L, totalBeds - occupiedBeds);

        long activeAllocations = allocationRepository.countByActiveTrue();

        double occupancyPercentage = totalBeds == 0
                ? 0.0
                : Math.round(
                    ((double) occupiedBeds / totalBeds) * 10000.0
                ) / 100.0;

        return new DashboardResponse(
                totalStudents,
                activeStudents,
                totalRooms,
                activeRooms,
                totalBeds,
                occupiedBeds,
                availableBeds,
                activeAllocations,
                occupancyPercentage
        );
    }
}
