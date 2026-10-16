package com.hostel.management.controller;

import com.hostel.management.entity.*;
import com.hostel.management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.system-admin.email=sysadmin-test@example.com")
@AutoConfigureMockMvc
class StudentDeletionIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FeeRecordRepository feeRecordRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "sysadmin-test@example.com";

    @BeforeEach
    void setUpAdmin() {
        if (userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).isEmpty()) {
            userRepository.save(new User("System Admin", ADMIN_EMAIL, passwordEncoder.encode("admin-pass"), UserRole.ADMIN));
        }
    }

    private Student createStudentWithUser(String email, String studentCode) {
        User user = new User("Test Student " + studentCode, email, passwordEncoder.encode("student-pass"), UserRole.STUDENT, "Hostel A");
        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setUser(savedUser);
        student.setStudentCode(studentCode);
        student.setCourse("Computer Science");
        student.setStudyYear(2);
        student.setPhone("9876543210");
        student.setHostelName("Hostel A");
        student.setAdmissionDate(LocalDate.now());
        student.setActive(true);
        return studentRepository.save(student);
    }

    @Test
    void deleteStudentWithoutFeesSucceeds() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        Student student = createStudentWithUser("del1-" + suffix + "@example.com", "STU1-" + suffix);
        Long studentId = student.getId();
        Long userId = student.getUser().getId();

        mvc.perform(delete("/api/students/{id}", studentId)
                        .with(jwt().jwt(j -> j.subject(ADMIN_EMAIL)).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        assertFalse(studentRepository.existsById(studentId));
        assertFalse(userRepository.existsById(userId));
    }

    @Test
    void deleteStudentWithDuePaymentIsBlockedWithConflict() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        Student student = createStudentWithUser("del2-" + suffix + "@example.com", "STU2-" + suffix);
        Long studentId = student.getId();

        FeeRecord fee = new FeeRecord();
        fee.setStudent(student);
        fee.setFeeType("Hostel Fee");
        fee.setTotalAmount(new BigDecimal("5000.00"));
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setDueDate(LocalDate.now().plusMonths(1));
        fee.setStatus(FeeRecord.FeeStatus.PENDING);
        FeeRecord savedFee = feeRecordRepository.save(fee);

        mvc.perform(delete("/api/students/{id}", studentId)
                        .with(jwt().jwt(j -> j.subject(ADMIN_EMAIL)).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete this student because they have pending or due fee payments. Clear all dues before deleting."));

        assertTrue(studentRepository.existsById(studentId));
        assertTrue(feeRecordRepository.existsById(savedFee.getId()));
    }

    @Test
    void deleteStudentWithPartialPaymentIsBlockedWithConflict() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        Student student = createStudentWithUser("del3-" + suffix + "@example.com", "STU3-" + suffix);
        Long studentId = student.getId();

        FeeRecord fee = new FeeRecord();
        fee.setStudent(student);
        fee.setFeeType("Mess Fee");
        fee.setTotalAmount(new BigDecimal("4000.00"));
        fee.setPaidAmount(new BigDecimal("2000.00"));
        fee.setDueDate(LocalDate.now().plusMonths(1));
        fee.setStatus(FeeRecord.FeeStatus.PARTIAL);
        FeeRecord savedFee = feeRecordRepository.save(fee);

        Payment payment = new Payment();
        payment.setFeeRecord(savedFee);
        payment.setAmount(new BigDecimal("2000.00"));
        payment.setPaymentMethod("UPI");
        paymentRepository.save(payment);

        mvc.perform(delete("/api/students/{id}", studentId)
                        .with(jwt().jwt(j -> j.subject(ADMIN_EMAIL)).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete this student because they have pending or due fee payments. Clear all dues before deleting."));

        assertTrue(studentRepository.existsById(studentId));
    }

    @Test
    void deleteStudentWithSettledPaymentSucceedsAndCascades() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        Student student = createStudentWithUser("del4-" + suffix + "@example.com", "STU4-" + suffix);
        Long studentId = student.getId();
        Long userId = student.getUser().getId();

        FeeRecord fee = new FeeRecord();
        fee.setStudent(student);
        fee.setFeeType("Semester Rent");
        fee.setTotalAmount(new BigDecimal("6000.00"));
        fee.setPaidAmount(new BigDecimal("6000.00"));
        fee.setDueDate(LocalDate.now().plusMonths(1));
        fee.setStatus(FeeRecord.FeeStatus.PAID);
        FeeRecord savedFee = feeRecordRepository.save(fee);

        Payment payment = new Payment();
        payment.setFeeRecord(savedFee);
        payment.setAmount(new BigDecimal("6000.00"));
        payment.setPaymentMethod("UPI");
        Payment savedPayment = paymentRepository.save(payment);

        mvc.perform(delete("/api/students/{id}", studentId)
                        .with(jwt().jwt(j -> j.subject(ADMIN_EMAIL)).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        assertFalse(studentRepository.existsById(studentId));
        assertFalse(userRepository.existsById(userId));
        assertFalse(feeRecordRepository.existsById(savedFee.getId()));
        assertFalse(paymentRepository.existsById(savedPayment.getId()));
    }
}
