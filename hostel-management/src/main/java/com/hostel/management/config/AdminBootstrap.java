
package com.hostel.management.config;

import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    name = "app.bootstrap-admin.enabled",
    havingValue = "true"
)
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.name}")
    private String adminName;

    @Value("${app.bootstrap-admin.email}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password}")
    private String adminPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String cleanEmail = adminEmail != null && !adminEmail.isBlank()
                ? adminEmail.trim()
                : "Premchand@gmail.com";

        String cleanName = adminName != null && !adminName.isBlank()
                ? adminName.trim()
                : "System Administrator";

        String rawPassword = adminPassword != null
                ? adminPassword.trim().replaceAll("^[\"']|[\"']$", "").trim()
                : "";

        if (rawPassword.length() < 8 || rawPassword.length() > 72) {
            rawPassword = "Admin@12345678";
        }

        var existingUser = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (existingUser.isPresent()) {
            System.out.println(
                "Admin bootstrap skipped: account '" + cleanEmail + "' already exists."
            );
            return;
        }

        User admin = new User();
        admin.setFullName(cleanName);
        admin.setEmail(cleanEmail);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);

        System.out.println(
            "Initial ADMIN account '" + cleanEmail + "' created successfully."
        );
    }
}
