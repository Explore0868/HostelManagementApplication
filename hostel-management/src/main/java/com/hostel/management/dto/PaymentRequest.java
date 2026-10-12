
package com.hostel.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(

    @NotNull(message = "Fee record ID is required")
    @Positive(message = "Fee record ID must be positive")
    Long feeRecordId,

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment must be greater than zero")
    BigDecimal amount,

    String paymentMethod,

    String transactionReference
) {}
