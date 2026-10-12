
package com.hostel.management.config;

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

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET must be a valid Base64 value", ex);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must decode to at least 32 bytes");
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
