package com.hostel.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHostelRequest(
        @NotBlank @Size(max = 100) String hostelName,
        @NotBlank @Size(max = 100) String ownerName,
        @NotBlank @Email @Size(max = 150) String ownerEmail,
        @NotBlank @Size(min = 8, max = 72) String initialPassword,
        @Size(max = 300) String address,
        @Size(max = 40) String contact) {}
