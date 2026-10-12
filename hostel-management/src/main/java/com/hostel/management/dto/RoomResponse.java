
package com.hostel.management.dto;

public record RoomResponse(
        Long id,
        String roomNumber,
        String roomType,
        String blockName,
        Integer capacity,
        long occupiedBeds,
        long availableBeds,
        boolean active,
        java.math.BigDecimal monthlyRent,
        String description,
        String hostelName
) {}
