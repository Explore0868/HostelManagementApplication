
package com.hostel.management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeRecordResponse(
    Long id,
    Long studentId,
    String studentCode,
    String studentName,
    String feeType,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal balanceAmount,
    LocalDate dueDate,
    String status
) {}
