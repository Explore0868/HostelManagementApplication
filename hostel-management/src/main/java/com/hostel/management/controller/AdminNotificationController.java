package com.hostel.management.controller;

import com.hostel.management.entity.AdminNotification;
import com.hostel.management.repository.AdminNotificationRepository;
import com.hostel.management.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final AdminNotificationRepository repository;
    private final SecurityUtils security;
    public AdminNotificationController(AdminNotificationRepository repository, SecurityUtils security) {
        this.repository = repository; this.security = security;
    }
    @GetMapping public List<AdminNotification> list() { requireSystemAdmin(); return repository.findAllByOrderByCreatedAtDesc(); }
    @PatchMapping("/{id}/read")
    public AdminNotification markRead(@PathVariable Long id) {
        requireSystemAdmin();
        AdminNotification event = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        event.setRead(true); return repository.save(event);
    }
    private void requireSystemAdmin() {
        if (!security.isSuperAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the System Admin can view notifications.");
    }
}
