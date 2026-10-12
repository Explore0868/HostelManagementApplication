package com.hostel.management.controller;

import com.hostel.management.entity.OwnerNotification;
import com.hostel.management.repository.OwnerNotificationRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/owner/notifications")
@PreAuthorize("@securityUtils.isHostelOwner()")
public class OwnerNotificationController {
    private final OwnerNotificationRepository repository;
    private final SecurityUtils security;
    public OwnerNotificationController(OwnerNotificationRepository repository, SecurityUtils security) {
        this.repository = repository; this.security = security;
    }
    @GetMapping
    public List<OwnerNotification> list() {
        Long userId = security.getCurrentUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    @PatchMapping("/{id}/read")
    public OwnerNotification markRead(@PathVariable Long id) {
        Long userId = security.getCurrentUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
        OwnerNotification notification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.setRead(true);
        return repository.save(notification);
    }
}
