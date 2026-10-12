package com.hostel.management.dto;

import java.time.LocalDate;

public record StudentProfileResponse(
        Long studentId,
        Long userId,
        String studentCode,
        String fullName,
        String email,
        String phone,
        String course,
        Integer studyYear,
        String guardianName,
        String guardianPhone,
        LocalDate admissionDate,
        boolean active,
        String hostelName,
        AllocatedRoomInfo roomAllocation
) {
    public record AllocatedRoomInfo(
            Long allocationId,
            Long roomId,
            String roomNumber,
            String roomType,
            String blockName,
            Integer bedNumber,
            LocalDate allocationDate
    ) {}
}
