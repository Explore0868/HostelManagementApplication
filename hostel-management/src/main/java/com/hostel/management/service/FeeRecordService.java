package com.hostel.management.service;

import com.hostel.management.dto.FeeRecordRequest;
import com.hostel.management.dto.FeeRecordResponse;
import com.hostel.management.dto.PaymentRequest;
import com.hostel.management.dto.PaymentResponse;
import com.hostel.management.entity.FeeRecord;
import com.hostel.management.entity.Payment;
import com.hostel.management.entity.Student;
import com.hostel.management.repository.FeeRecordRepository;
import com.hostel.management.repository.PaymentRepository;
import com.hostel.management.repository.StudentRepository;
import com.hostel.management.security.SecurityUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FeeRecordService {

    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final SecurityUtils securityUtils;

    public FeeRecordService(
            FeeRecordRepository feeRecordRepository,
            PaymentRepository paymentRepository,
            StudentRepository studentRepository,
            StudentService studentService,
            SecurityUtils securityUtils) {
        this.feeRecordRepository = feeRecordRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.studentService = studentService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public FeeRecordResponse createFee(FeeRecordRequest request) {

        if (request.studentId() == null) {
            throw new IllegalArgumentException("Student ID is required");
        }

        if (request.totalAmount() == null
                || request.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fee amount must be greater than zero");
        }

        if (request.feeType() == null || request.feeType().isBlank()) {
            throw new IllegalArgumentException("Fee type is required");
        }

        if (request.dueDate() == null) {
            throw new IllegalArgumentException("Due date is required");
        }

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found"));

        securityUtils.enforceHostelAccess(studentService.getStudentHostel(student));

        FeeRecord fee = new FeeRecord();
        fee.setStudent(student);
        fee.setFeeType(request.feeType().trim());
        fee.setTotalAmount(request.totalAmount());
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setDueDate(request.dueDate());
        fee.setStatus(FeeRecord.FeeStatus.PENDING);

        FeeRecord saved = feeRecordRepository.save(fee);

        return toFeeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> getAllFees() {
        if (securityUtils.isSuperAdmin()) {
            return feeRecordRepository.findAllByOrderByDueDateAsc().stream()
                    .map(this::toFeeResponse)
                    .toList();
        }
        return feeRecordRepository.findAllByOrderByDueDateAsc()
                .stream()
                .filter(f -> securityUtils.canAccessHostel(studentService.getStudentHostel(f.getStudent())))
                .map(this::toFeeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeeRecordResponse getFeeById(Long id) {
        FeeRecord fee = feeRecordRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Fee record not found"));

        securityUtils.enforceHostelAccess(studentService.getStudentHostel(fee.getStudent()));
        return toFeeResponse(fee);
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> getFeesByStudent(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        securityUtils.enforceStudentSelfAccess(student.getUser().getId());

        if (!securityUtils.getCurrentUser()
                .map(u -> u.getId().equals(student.getUser().getId()))
                .orElse(false)) {
            securityUtils.enforceHostelAccess(studentService.getStudentHostel(student));
        }

        return feeRecordRepository
                .findByStudent_IdOrderByDueDateDesc(studentId)
                .stream()
                .map(this::toFeeResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {

        if (request.feeRecordId() == null) {
            throw new IllegalArgumentException("Fee record ID is required");
        }

        if (request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        FeeRecord fee = feeRecordRepository
                .findByIdForUpdate(request.feeRecordId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Fee record not found"));

        securityUtils.enforceHostelAccess(studentService.getStudentHostel(fee.getStudent()));

        BigDecimal balance = fee.getTotalAmount()
                .subtract(fee.getPaidAmount());

        if (request.amount().compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    "Payment exceeds the outstanding balance of " + balance);
        }

        Payment payment = new Payment();
        payment.setFeeRecord(fee);
        payment.setAmount(request.amount());
        payment.setPaymentMethod(
                request.paymentMethod() == null
                        ? null : request.paymentMethod().trim());
        payment.setTransactionReference(
                request.transactionReference() == null
                        ? null : request.transactionReference().trim());

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal updatedPaidAmount =
                fee.getPaidAmount().add(request.amount());

        fee.setPaidAmount(updatedPaidAmount);

        if (updatedPaidAmount.compareTo(fee.getTotalAmount()) >= 0) {
            fee.setStatus(FeeRecord.FeeStatus.PAID);
        } else {
            fee.setStatus(FeeRecord.FeeStatus.PARTIAL);
        }

        feeRecordRepository.save(fee);

        return new PaymentResponse(
                savedPayment.getId(),
                fee.getId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentMethod(),
                savedPayment.getTransactionReference(),
                savedPayment.getPaymentDate()
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForFee(Long feeRecordId) {

        FeeRecord fee = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Fee record not found"));

        securityUtils.enforceStudentSelfAccess(fee.getStudent().getUser().getId());

        securityUtils.enforceHostelAccess(studentService.getStudentHostel(fee.getStudent()));

        return paymentRepository
                .findByFeeRecord_IdOrderByPaymentDateDesc(feeRecordId)
                .stream()
                .map(payment -> new PaymentResponse(
                        payment.getId(),
                        payment.getFeeRecord().getId(),
                        payment.getAmount(),
                        payment.getPaymentMethod(),
                        payment.getTransactionReference(),
                        payment.getPaymentDate()
                ))
                .toList();
    }

    private FeeRecordResponse toFeeResponse(FeeRecord fee) {

        Student student = fee.getStudent();

        String studentName = student.getUser() != null
                ? student.getUser().getFullName()
                : "";

        BigDecimal paidAmount = fee.getPaidAmount() != null
                ? fee.getPaidAmount() : BigDecimal.ZERO;

        BigDecimal balanceAmount =
                fee.getTotalAmount().subtract(paidAmount);

        return new FeeRecordResponse(
                fee.getId(),
                student.getId(),
                student.getStudentCode(),
                studentName,
                fee.getFeeType(),
                fee.getTotalAmount(),
                paidAmount,
                balanceAmount,
                fee.getDueDate(),
                fee.getStatus().name()
        );
    }
}
