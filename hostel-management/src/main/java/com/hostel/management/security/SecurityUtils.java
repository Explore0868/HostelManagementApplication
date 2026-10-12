package com.hostel.management.security;

import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Locale;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;
    private final String systemAdminEmail;

    public SecurityUtils(UserRepository userRepository,
                         @Value("${app.system-admin.email:Premchand@gmail.com}") String systemAdminEmail) {
        this.userRepository = userRepository;
        this.systemAdminEmail = systemAdminEmail;
    }

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        String email = null;
        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            email = jwt.getSubject();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (auth.getName() != null && !auth.getName().isBlank()) {
            email = auth.getName();
        }

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public String getCurrentUserHostel() {
        // The persisted account is authoritative. A token may outlive a
        // hostel reassignment, so its hostel claim must never grant access.
        return getCurrentUser()
                .map(User::getHostelName)
                .filter(h -> h != null && !h.isBlank())
                .orElse(null);
    }

    public boolean isSuperAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.ADMIN
                        && u.getEmail() != null
                        && u.getEmail().equalsIgnoreCase(systemAdminEmail.trim()))
                .orElse(false);
    }

    public boolean isHostelOwner() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.ADMIN
                        && u.getHostelName() != null
                        && !u.getHostelName().isBlank())
                .orElse(false);
    }

    public void enforceStudentSelfAccess(Long studentUserId) {
        User current = getCurrentUser().orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User account not found"));
        if (current.getRole() == UserRole.STUDENT
                && (studentUserId == null || !studentUserId.equals(current.getId()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Students can only view their own records.");
        }
    }

    public boolean canAccessHostel(String targetHostel) {
        String adminHostel = getCurrentUserHostel();
        if (isSuperAdmin()) {
            return true;
        }

        if (adminHostel == null || adminHostel.isBlank()
                || targetHostel == null || targetHostel.isBlank()) {
            return false;
        }

        String normAdmin = normalizeHostel(adminHostel);
        String normTarget = normalizeHostel(targetHostel);

        return normAdmin.equalsIgnoreCase(normTarget);
    }

    public void enforceHostelAccess(String targetHostel) {
        if (!canAccessHostel(targetHostel)) {
            String adminHostel = getCurrentUserHostel();
            String target = targetHostel == null || targetHostel.isBlank()
                    ? "an unassigned hostel" : targetHostel;
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format(
                        "Hostel isolation violation: Administrator of '%s' cannot access or modify data belonging to '%s'.",
                        adminHostel == null || adminHostel.isBlank() ? "unassigned" : adminHostel,
                        target
                    )
            );
        }
    }

    public static String normalizeHostel(String hostel) {
        if (hostel == null) return "";
        return hostel.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
