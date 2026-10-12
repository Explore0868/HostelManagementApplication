package com.hostel.management.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HostelResponse(Long id, String hostelId, String name, String address,
        String contact, Long ownerUserId, String ownerName, String ownerEmail,
        boolean ownerEnabled, List<String> blocks, long roomCount, long studentCount, LocalDateTime createdAt) {}
