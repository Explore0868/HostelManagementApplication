
package com.hostel.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeRecordRequest(

    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be positive")
    Long studentId,

    @NotBlank(message = "Fee type is required")
    @Size(max = 50, message = "Fee type cannot exceed 50 characters")
    String feeType,

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
    BigDecimal totalAmount,

    @NotNull(message = "Due date is required")
    LocalDate dueDate
) {}
