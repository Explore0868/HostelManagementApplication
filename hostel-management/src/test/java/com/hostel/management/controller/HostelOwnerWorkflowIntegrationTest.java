package com.hostel.management.controller;

import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.system-admin.email=system-admin-test@example.com")
@AutoConfigureMockMvc
class HostelOwnerWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void systemAdminCreatesOwnersAndOwnersAreIsolated() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String adminEmail = "system-admin-test@example.com";
        if (users.findByEmailIgnoreCase(adminEmail).isEmpty()) {
            users.save(new User("System Admin", adminEmail, encoder.encode("admin-password"), UserRole.ADMIN));
        }

        String hostelA = "Workflow A " + suffix;
        String hostelB = "Workflow B " + suffix;
        String emailA = "owner-a-" + suffix + "@example.com";
        String emailB = "owner-b-" + suffix + "@example.com";
        String createA = """
                {"hostelName":"%s","ownerName":"Owner A","ownerEmail":"%s","initialPassword":"temporary-A-123","address":"Address A","contact":"1111111111"}
                """.formatted(hostelA, emailA);
        String createB = """
                {"hostelName":"%s","ownerName":"Owner B","ownerEmail":"%s","initialPassword":"temporary-B-123","address":"Address B","contact":"2222222222"}
                """.formatted(hostelB, emailB);

        mvc.perform(post("/api/admin/hostels").with(jwt().jwt(j -> j.subject(adminEmail)).authorities(() -> "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content(createA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.hostelId").isNotEmpty())
                .andExpect(jsonPath("$.ownerEmail").value(emailA)).andExpect(jsonPath("$.initialPassword").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/admin/hostels").with(jwt().jwt(j -> j.subject(adminEmail)).authorities(() -> "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content(createB))
                .andExpect(status().isCreated());

        String tokenA = login(emailA, "temporary-A-123", true);
        mvc.perform(get("/api/dashboard/stats").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
        changePassword(emailA, tokenA, "temporary-A-123", "permanent-A-123");
        String ownerTokenA = login(emailA, "permanent-A-123", false);

        String tokenB = login(emailB, "temporary-B-123", true);
        changePassword(emailB, tokenB, "temporary-B-123", "permanent-B-123");
        String ownerTokenB = login(emailB, "permanent-B-123", false);

        mvc.perform(post("/api/rooms").header("Authorization", "Bearer " + ownerTokenA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(("{\"roomNumber\":\"A-%s\",\"roomType\":\"Single\",\"blockName\":\"North\",\"hostelName\":\"%s\",\"capacity\":1}").formatted(suffix, hostelA)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/rooms").header("Authorization", "Bearer " + ownerTokenB))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("A-" + suffix))));
        mvc.perform(get("/api/rooms").header("Authorization", "Bearer " + ownerTokenA))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("A-" + suffix)));

        String listResult = mvc.perform(get("/api/admin/hostels").with(jwt().jwt(j -> j.subject(adminEmail)).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<Number> ids = com.jayway.jsonpath.JsonPath.read(listResult, "$[?(@.name == '" + hostelB + "')].id");
        Number hostelBId = ids.get(0);

        mvc.perform(delete("/api/admin/hostels/" + hostelBId + "?confirm=true").with(jwt().jwt(j -> j.subject(adminEmail)).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNoContent());
    }

    private String login(String email, String password, boolean mustChange) throws Exception {
        String result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(("{\"email\":\"%s\",\"password\":\"%s\"}").formatted(email, password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.mustChangePassword").value(mustChange))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(result, "$.accessToken");
    }

    private void changePassword(String email, String token, String oldPassword, String newPassword) throws Exception {
        mvc.perform(put("/api/account/password").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}").formatted(oldPassword, newPassword)))
                .andExpect(status().isNoContent());
    }
}
