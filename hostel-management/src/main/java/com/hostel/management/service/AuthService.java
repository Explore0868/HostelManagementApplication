
package com.hostel.management.service;

import java.time.Instant;

import com.hostel.management.dto.LoginRequest;
import com.hostel.management.dto.LoginResponse;
import com.hostel.management.entity.User;
import com.hostel.management.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;
    private final String systemAdminEmail;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${jwt.expiration-seconds:3600}") long expirationSeconds,
            @Value("${app.system-admin.email:Premchand@gmail.com}") String systemAdminEmail) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
        this.systemAdminEmail = systemAdminEmail;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()
                || !passwordEncoder.matches(
                        request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        String role = user.getRole().name();

        String hostelName = user.getHostelName() != null ? user.getHostelName() : "";

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("hostel-management")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", role)
                .claim("mustChangePassword", user.isMustChangePassword());

        if (!hostelName.isBlank()) {
            claimsBuilder.claim("hostelName", hostelName);
        }

        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new LoginResponse(
                token,
                "Bearer",
                expirationSeconds,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                role,
                user.getHostelName(),
                user.isMustChangePassword(),
                user.getRole() == com.hostel.management.entity.UserRole.ADMIN
                        && user.getEmail().equalsIgnoreCase(systemAdminEmail.trim())
        );
    }
}
