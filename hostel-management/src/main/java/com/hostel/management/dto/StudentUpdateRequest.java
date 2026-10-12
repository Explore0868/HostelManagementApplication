
package com.hostel.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentUpdateRequest(

        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(max = 30)
        String studentCode,

        @NotBlank
        @Pattern(regexp = "^[0-9+() -]{7,20}$")
        @Size(max = 20)
        String phone,

        @NotBlank
        @Size(max = 100)
        String course,

        @NotNull
        @Min(1)
        @Max(5)
        Integer studyYear,

        @NotBlank
        @Size(max = 100)
        String guardianName,

        @NotBlank
        @Size(max = 20)
        String guardianPhone,

        LocalDate admissionDate
) {}
