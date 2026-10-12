package com.hostel.management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedAccessToAnnouncementsReturns401() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateAnnouncements() throws Exception {
        mockMvc.perform(
                post("/api/announcements")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Notice\",\"content\":\"Test\",\"category\":\"General\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotMarkAttendance() throws Exception {
        mockMvc.perform(
                post("/api/attendance")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"studentId\":1,\"attendanceDate\":\"2026-10-11\",\"status\":\"PRESENT\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateFees() throws Exception {
        mockMvc.perform(
                post("/api/fees")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"studentId\":1,\"amount\":5000,\"dueDate\":\"2026-12-01\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanReadAnnouncements() throws Exception {
        mockMvc.perform(
                get("/api/announcements")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                    )
            )
            .andExpect(status().isOk());
    }
}
