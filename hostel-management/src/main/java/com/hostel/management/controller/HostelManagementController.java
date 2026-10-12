package com.hostel.management.controller;

import com.hostel.management.dto.CreateHostelRequest;
import com.hostel.management.dto.HostelResponse;
import com.hostel.management.service.HostelManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/hostels")
public class HostelManagementController {
    private final HostelManagementService service;
    public HostelManagementController(HostelManagementService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public HostelResponse create(@Valid @RequestBody CreateHostelRequest request) { return service.create(request); }
    @GetMapping public List<HostelResponse> list() { return service.list(); }
    @PatchMapping("/{id}/name")
    public HostelResponse rename(@PathVariable Long id, @RequestBody Map<String, String> body) { return service.rename(id, body.get("name")); }
    @PatchMapping("/{id}/owner-status")
    public HostelResponse ownerStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return service.setOwnerEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
    }
    @PatchMapping("/{id}/owner")
    public HostelResponse updateOwner(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.updateOwner(id, body.get("name"), body.get("email"));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam boolean confirm) { service.deleteIfEmpty(id, confirm); }
}
