
package com.hostel.management.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.hostel.management.entity.User;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.SecurityUtils;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${jwt.secret}") String base64Secret) {

        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured and cannot be blank");
        }

        String cleaned = base64Secret.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException ex1) {
            try {
                keyBytes = Base64.getUrlDecoder().decode(cleaned);
            } catch (IllegalArgumentException ex2) {
                byte[] rawBytes = cleaned.getBytes(StandardCharsets.UTF_8);
                if (rawBytes.length >= 32) {
                    keyBytes = rawBytes;
                } else {
                    throw new IllegalStateException(
                            "JWT_SECRET must be a valid Base64 value or at least 32 UTF-8 characters", ex1);
                }
            }
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must decode to at least 32 bytes (256 bits) for HMAC-SHA256.");
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            UserRepository userRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> accountValidator = jwt -> {
            User user = userRepository.findByEmailIgnoreCase(jwt.getSubject())
                    .orElse(null);
            if (user == null || !user.isEnabled()) {
                return invalidToken("Account is missing or disabled.");
            }

            String tokenRole = jwt.getClaimAsString("role");
            if (tokenRole == null || !user.getRole().name().equalsIgnoreCase(
                    tokenRole.replaceFirst("^ROLE_", ""))) {
                return invalidToken("Account permissions have changed. Sign in again.");
            }

            String tokenHostel = jwt.getClaimAsString("hostelName");
            if (!SecurityUtils.normalizeHostel(user.getHostelName()).equals(
                    SecurityUtils.normalizeHostel(tokenHostel))) {
                return invalidToken("Account hostel assignment has changed. Sign in again.");
            }

            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("hostel-management"),
                accountValidator));
        return decoder;
    }

    private OAuth2TokenValidatorResult invalidToken(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }
}
