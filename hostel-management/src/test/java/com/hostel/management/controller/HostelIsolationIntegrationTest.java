package com.hostel.management.controller;

import com.hostel.management.entity.User;
import com.hostel.management.entity.UserRole;
import com.hostel.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HostelIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupUsers() {
        if (userRepository.findByEmailIgnoreCase("adminA@hostel.com").isEmpty()) {
            User adminA = new User("Admin A", "adminA@hostel.com", passwordEncoder.encode("password123"), UserRole.ADMIN, "Hostel A");
            userRepository.save(adminA);
        }

        if (userRepository.findByEmailIgnoreCase("adminB@hostel.com").isEmpty()) {
            User adminB = new User("Admin B", "adminB@hostel.com", passwordEncoder.encode("password123"), UserRole.ADMIN, "Hostel B");
            userRepository.save(adminB);
        }
    }

    @Test
    void adminOfHostelACannotCreateRoomForHostelB() throws Exception {
        mockMvc.perform(
                post("/api/rooms")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("adminA@hostel.com").claim("role", "ADMIN").claim("hostelName", "Hostel A"))
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"roomNumber\":\"B-999\",\"roomType\":\"Single\",\"blockName\":\"North Wing\",\"hostelName\":\"Hostel B\",\"capacity\":1}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void adminOfHostelACanCreateRoomForHostelA() throws Exception {
        String testRoom = "A-TEST-" + (System.currentTimeMillis() % 100000);
        mockMvc.perform(
                post("/api/rooms")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("adminA@hostel.com").claim("role", "ADMIN").claim("hostelName", "Hostel A"))
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"roomNumber\":\"" + testRoom + "\",\"roomType\":\"Double\",\"blockName\":\"North Wing\",\"hostelName\":\"Hostel A\",\"capacity\":2}")
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.roomNumber").value(testRoom));
    }

    @Test
    void adminOfHostelACannotCreateAdministratorForHostelB() throws Exception {
        mockMvc.perform(
                post("/api/admin/accounts")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("adminA@hostel.com")
                                .claim("role", "ADMIN")
                                .claim("hostelName", "Hostel A")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fullName\":\"Hostel B Admin\",\"email\":\"admin-b-child@hostel.com\",\"password\":\"password123\",\"hostelName\":\"Hostel B\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void adminOfHostelAOnlySeesHostelARooms() throws Exception {
        mockMvc.perform(
                get("/api/rooms")
                    .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("adminA@hostel.com").claim("role", "ADMIN").claim("hostelName", "Hostel A"))
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].hostelName").value(org.hamcrest.Matchers.everyItem(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo("Hostel B")))));
    }
}
