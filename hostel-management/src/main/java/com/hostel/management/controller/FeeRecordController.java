
package com.hostel.management.controller;

import com.hostel.management.dto.FeeRecordRequest;
import com.hostel.management.dto.FeeRecordResponse;
import com.hostel.management.dto.PaymentRequest;
import com.hostel.management.dto.PaymentResponse;
import com.hostel.management.service.FeeRecordService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees")
public class FeeRecordController {

    private final FeeRecordService feeRecordService;

    public FeeRecordController(FeeRecordService feeRecordService) {
        this.feeRecordService = feeRecordService;
    }

    // Create a fee record
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeeRecordResponse createFee(
            @Valid @RequestBody FeeRecordRequest request) {
        return feeRecordService.createFee(request);
    }

    // Get all fee records
    @GetMapping
    public List<FeeRecordResponse> getAllFees() {
        return feeRecordService.getAllFees();
    }

    // Get one fee record
    @GetMapping("/{id}")
    public FeeRecordResponse getFeeById(@PathVariable Long id) {
        return feeRecordService.getFeeById(id);
    }

    // Get fee records for one student
    @GetMapping("/student/{studentId}")
    public List<FeeRecordResponse> getFeesByStudent(
            @PathVariable Long studentId) {
        return feeRecordService.getFeesByStudent(studentId);
    }

    // Record a payment against a fee
    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse recordPayment(
            @Valid @RequestBody PaymentRequest request) {
        return feeRecordService.recordPayment(request);
    }

    // Get payment history for a fee record
    @GetMapping("/{feeRecordId}/payments")
    public List<PaymentResponse> getPaymentsForFee(
            @PathVariable Long feeRecordId) {
        return feeRecordService.getPaymentsForFee(feeRecordId);
    }
}
