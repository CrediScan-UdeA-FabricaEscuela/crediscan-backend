package co.udea.codefactory.creditscoring.shared.security.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Task 7.8 — LastAdminProtectionIT
 * Given exactly one ADMIN user (seeded by V15), PATCH /api/v1/auth/usuarios/{id}/rol returns 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class LastAdminProtectionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // V15 seeds exactly one ADMIN: a0000000-0000-0000-0000-000000000001
    private static final String SEEDED_ADMIN_ID = "a0000000-0000-0000-0000-000000000001";

    @Test
    void changeLastAdminRole_shouldReturn409() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/usuarios/" + SEEDED_ADMIN_ID + "/rol")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"ANALYST\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("No se puede modificar el último administrador del sistema"));
    }

    @Test
    void nonAdminCannotChangeRoles_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/usuarios/" + SEEDED_ADMIN_ID + "/rol")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"ANALYST\"}"))
                .andExpect(status().isForbidden());
    }
}
