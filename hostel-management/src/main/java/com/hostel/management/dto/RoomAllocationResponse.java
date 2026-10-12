
package com.hostel.management.dto;

import java.time.LocalDate;

public record RoomAllocationResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Long roomId,
        String roomNumber,
        Integer bedNumber,
        LocalDate allocationDate,
        LocalDate vacatedDate,
        boolean active
) {}
