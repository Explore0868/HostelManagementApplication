
package com.hostel.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record RoomRequest(

        @NotBlank(message = "Room number is required")
        @Size(max = 20, message = "Room number cannot exceed 20 characters")
        String roomNumber,

        @NotBlank(message = "Room type is required")
        @Size(max = 40, message = "Room type cannot exceed 40 characters")
        String roomType,

        @NotBlank(message = "Block name is required")
        @Size(max = 50, message = "Block name cannot exceed 50 characters")
        String blockName,

        @NotNull(message = "Room capacity is required")
        @Min(value = 1, message = "Room capacity must be at least 1")
        Integer capacity,

        @DecimalMin(value = "0.00", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Size(max = 100, message = "Hostel name cannot exceed 100 characters")
        String hostelName

) {}
