package com.hostel.management.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String fullName,
        String email,
        String role,
        String hostelName,
        boolean mustChangePassword,
        boolean systemAdmin
) {}
