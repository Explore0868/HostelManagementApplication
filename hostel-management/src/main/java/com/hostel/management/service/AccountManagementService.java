
package com.hostel.management.service;

import com.hostel.management.dto.ChangePasswordRequest;
import com.hostel.management.dto.CreateAdminRequest;
import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.SecurityUtils;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    public AccountManagementService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public void createAdmin(CreateAdminRequest request) {
        String email = request.email().trim();
        String targetHostel = request.hostelName().trim();
        securityUtils.enforceHostelAccess(targetHostel);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        User admin = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.ADMIN,
                targetHostel
        );

        admin.setEnabled(true);
        userRepository.save(admin);
    }

    @Transactional
    public void changeOwnPassword(
            String authenticatedEmail,
            ChangePasswordRequest request) {

        User user = userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated account was not found."
                ));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is disabled."
            );
        }

        if (!passwordEncoder.matches(
                request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a password different from your current password."
            );
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
