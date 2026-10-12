
package com.hostel.management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
    Long id,
    Long feeRecordId,
    BigDecimal amount,
    String paymentMethod,
    String transactionReference,
    LocalDateTime paymentDate
) {}
