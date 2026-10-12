package com.hostel.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**", "/api/health", "/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Announcements: Any authenticated user can read; only ADMIN can modify
                .requestMatchers(HttpMethod.GET, "/api/announcements/**").authenticated()
                .requestMatchers("/api/announcements/**").hasRole("ADMIN")

                // Attendance: Students can view their own; only ADMIN can mark or view all
                .requestMatchers(HttpMethod.GET, "/api/attendance/student/**").authenticated()
                .requestMatchers("/api/attendance/**").hasRole("ADMIN")

                // Fees: Students can view their own; only ADMIN can create fees or record payments
                .requestMatchers(HttpMethod.GET, "/api/fees/student/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/fees/*/payments").authenticated()
                .requestMatchers("/api/fees/**").hasRole("ADMIN")

                // Students: Only ADMIN can view or manage student records
                .requestMatchers("/api/students/**").hasRole("ADMIN")

                // Rooms: All authenticated users can view rooms; only ADMIN can manage
                .requestMatchers(HttpMethod.GET, "/api/rooms/**").authenticated()
                .requestMatchers("/api/rooms/**").hasRole("ADMIN")

                // Room allocations: Only ADMIN
                .requestMatchers("/api/room-allocations/**").hasRole("ADMIN")

                // Dashboard stats: Only ADMIN
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")

                // Admin-specific operations: Only ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Student portal (self-service profile, attendance, fees): Authenticated
                .requestMatchers("/api/student/**").authenticated()

                // Account settings (e.g. self password change): Authenticated
                .requestMatchers("/api/account/**").authenticated()

                // Default: require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromRoleClaim);

        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFromRoleClaim(Jwt jwt) {
        Boolean mustChangePassword = jwt.getClaimAsBoolean("mustChangePassword");
        if (Boolean.TRUE.equals(mustChangePassword)) {
            return List.of(new SimpleGrantedAuthority("ROLE_PASSWORD_CHANGE_REQUIRED"));
        }

        Object roleClaim = jwt.getClaims().get("role");
        Collection<?> roles = roleClaim instanceof Collection<?> values
                ? values
                : roleClaim instanceof String role ? List.of(role) : List.of();

        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Object roleValue : roles) {
            if (!(roleValue instanceof String role) || role.isBlank()) {
                continue;
            }
            String normalized = role.trim().toUpperCase(Locale.ROOT);
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }
            authorities.add(new SimpleGrantedAuthority(normalized));
        }
        return authorities;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(origin -> origin.replaceAll("/+$", ""))
                .collect(Collectors.toList());

        if (origins.isEmpty()) {
            origins = List.of("http://localhost:5173");
        }

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
        );
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
