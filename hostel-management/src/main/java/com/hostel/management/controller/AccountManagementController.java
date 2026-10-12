
package com.hostel.management.controller;

import com.hostel.management.dto.ChangePasswordRequest;
import com.hostel.management.dto.CreateAdminRequest;
import com.hostel.management.service.AccountManagementService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountManagementController {

    private final AccountManagementService accountManagementService;

    public AccountManagementController(
            AccountManagementService accountManagementService) {
        this.accountManagementService = accountManagementService;
    }

    // Only an ADMIN can reach this endpoint; see SecurityConfig.
    @PostMapping("/api/admin/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public void createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        accountManagementService.createAdmin(request);
    }

    // Any authenticated user can change only their own password.
    @PutMapping("/api/account/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {

        accountManagementService.changeOwnPassword(
                jwt.getSubject(), request);
    }
}
