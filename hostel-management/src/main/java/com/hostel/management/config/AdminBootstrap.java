
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

        if (adminEmail == null || adminEmail.isBlank()
                || adminName == null || adminName.isBlank()
                || adminPassword == null || adminPassword.length() < 8
                || adminPassword.length() > 72) {
            throw new IllegalStateException(
                "Admin bootstrap requires a name, email, and password "
                + "between 8 and 72 characters."
            );
        }

        var existingUser = userRepository.findByEmailIgnoreCase(
            adminEmail.trim()
        );

        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                    "The configured admin email already belongs to a "
                    + "non-admin account. Use a different email."
                );
            }

            System.out.println(
                "Admin bootstrap skipped: this ADMIN account already exists."
            );
            return;
        }

        User admin = new User();
        admin.setFullName(adminName.trim());
        admin.setEmail(adminEmail.trim());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);

        System.out.println(
            "Initial ADMIN account created. Disable admin bootstrap now."
        );
    }
}
