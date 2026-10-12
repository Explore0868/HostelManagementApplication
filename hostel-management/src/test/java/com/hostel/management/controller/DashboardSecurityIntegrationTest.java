
package com.hostel.management.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardWithoutAuthenticationShouldReturn401()
            throws Exception {

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardWithStudentRoleShouldReturn403()
            throws Exception {

        mockMvc.perform(
                get("/api/dashboard/stats")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_STUDENT")
                        )
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardWithAdminRoleShouldReturn200()
            throws Exception {

        mockMvc.perform(
                get("/api/dashboard/stats")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                    )
            )
            .andExpect(status().isOk());
    }
}
