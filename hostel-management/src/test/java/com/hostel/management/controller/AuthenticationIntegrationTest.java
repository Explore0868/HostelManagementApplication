package com.hostel.management.controller;

import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_ADMIN_EMAIL = "test-auth-admin@example.com";
    private static final String TEST_STUDENT_EMAIL = "test-auth-student@example.com";
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUpUsers() {
        if (userRepository.findByEmailIgnoreCase(TEST_ADMIN_EMAIL).isEmpty()) {
            User admin = new User("Auth Test Admin", TEST_ADMIN_EMAIL, passwordEncoder.encode(TEST_PASSWORD), UserRole.ADMIN, "Alpha Hostel");
            admin.setEnabled(true);
            admin.setMustChangePassword(false);
            userRepository.save(admin);
        }

        if (userRepository.findByEmailIgnoreCase(TEST_STUDENT_EMAIL).isEmpty()) {
            User student = new User("Auth Test Student", TEST_STUDENT_EMAIL, passwordEncoder.encode(TEST_PASSWORD), UserRole.STUDENT, "Alpha Hostel");
            student.setEnabled(true);
            student.setMustChangePassword(false);
            userRepository.save(student);
        }
    }

    @Test
    @DisplayName("1. Correct credentials produce a successful login response with JWT token")
    void correctCredentialsProduceSuccessfulLoginResponse() throws Exception {
        String json = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(TEST_ADMIN_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
                .andExpect(jsonPath("$.email", equalTo(TEST_ADMIN_EMAIL)))
                .andExpect(jsonPath("$.role", equalTo("ADMIN")))
                .andExpect(jsonPath("$.mustChangePassword", equalTo(false)))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    @DisplayName("2. Incorrect credentials (wrong password or non-existent user) are rejected with 401")
    void incorrectCredentialsAreRejected() throws Exception {
        // Wrong password
        String wrongPasswordJson = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(TEST_ADMIN_EMAIL, "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPasswordJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo("Unauthorized")))
                .andExpect(jsonPath("$.message", equalTo("Invalid email or password")));

        // Non-existent user
        String nonExistentJson = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted("unknown-user@example.com", TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonExistentJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo("Unauthorized")));
    }

    @Test
    @DisplayName("3. Missing credentials or blank inputs are rejected with 400 Bad Request")
    void missingCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("Validation failed")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("Validation failed")));
    }

    @Test
    @DisplayName("4. A valid JWT can access a protected endpoint")
    void validJwtCanAccessProtectedEndpoint() throws Exception {
        String json = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(TEST_ADMIN_EMAIL, TEST_PASSWORD);
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");

        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. A missing JWT cannot access a protected endpoint and returns 401")
    void missingJwtCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. An expired or malformed JWT is rejected with 401")
    void invalidJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("7. A user with the wrong role receives a 403 Forbidden response")
    void wrongRoleReceivesForbiddenResponse() throws Exception {
        String studentJson = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(TEST_STUDENT_EMAIL, TEST_PASSWORD);
        String studentLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String studentToken = com.jayway.jsonpath.JsonPath.read(studentLoginResponse, "$.accessToken");

        // Students cannot access admin dashboard stats
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Students cannot access admin student management
        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. CORS preflight allows configured origin and headers")
    void corsAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("9. Public health endpoints are accessible without authentication")
    void publicHealthEndpointsAreAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")));

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")));
    }
}
