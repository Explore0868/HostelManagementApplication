
package com.hostel.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RoomAllocationRequest(

        @NotNull(message = "Student ID is required")
        @Positive(message = "Student ID must be positive")
        Long studentId,

        @NotNull(message = "Room ID is required")
        @Positive(message = "Room ID must be positive")
        Long roomId,

        @NotNull(message = "Bed number is required")
        @Positive(message = "Bed number must be positive")
        Integer bedNumber

) {}
