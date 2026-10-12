
package com.hostel.management.dto;

public record DashboardResponse(
        long totalStudents,
        long activeStudents,
        long totalRooms,
        long activeRooms,
        long totalBeds,
        long occupiedBeds,
        long availableBeds,
        long activeAllocations,
        double occupancyPercentage
) {
}
