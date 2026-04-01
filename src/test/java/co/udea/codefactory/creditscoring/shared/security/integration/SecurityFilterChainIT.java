package co.udea.codefactory.creditscoring.shared.security.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Task 7.6 — SecurityFilterChainIT
 * Validates 10 permission combos from the spec permission matrix using Spring Security's
 * user() test support (bypasses JWT filter; directly sets security context with given role).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SecurityFilterChainIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    // ANALYST -> POST /api/v1/solicitantes -> 201 (or 400 due to invalid payload — but not 403)
    @Test
    void analystCanPostSolicitantes() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalSolicitantePayload()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // We expect either 201 (created) or 400 (validation) — NOT 403
                    assert status != 403 : "ANALYST should not get 403 on POST /api/v1/solicitantes";
                });
    }

    // ANALYST -> GET /api/v1/reportes/distribución -> 403
    @Test
    void analystCannotGetReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/distribución")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tiene permisos para acceder a este recurso"));
    }

    // ANALYST -> POST /api/v1/variables-scoring -> 403
    @Test
    void analystCannotPostVariablesScoring() throws Exception {
        mockMvc.perform(post("/api/v1/variables-scoring")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // RISK_MANAGER -> GET /api/v1/reportes/distribución -> not 403
    @Test
    void riskManagerCanGetReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/distribución")
                        .with(user("rm").roles("RISK_MANAGER")))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "RISK_MANAGER should not get 403 on GET /api/v1/reportes/distribución";
                });
    }

    // RISK_MANAGER -> POST /api/v1/solicitantes -> 403
    @Test
    void riskManagerCannotPostSolicitantes() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("rm").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalSolicitantePayload()))
                .andExpect(status().isForbidden());
    }

    // ADMIN -> GET /api/v1/roles/permisos -> 200
    @Test
    void adminCanGetRolesPermisos() throws Exception {
        mockMvc.perform(get("/api/v1/roles/permisos")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // ANALYST -> GET /api/v1/roles/permisos -> 403
    @Test
    void analystCannotGetRolesPermisos() throws Exception {
        mockMvc.perform(get("/api/v1/roles/permisos")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    // CREDIT_SUPERVISOR -> POST /api/v1/variables-scoring -> 403
    @Test
    void creditSupervisorCannotPostVariablesScoring() throws Exception {
        mockMvc.perform(post("/api/v1/variables-scoring")
                        .with(user("cs").roles("CREDIT_SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ADMIN -> POST /api/v1/variables-scoring -> not 403
    @Test
    void adminCanPostVariablesScoring() throws Exception {
        mockMvc.perform(post("/api/v1/variables-scoring")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "ADMIN should not get 403 on POST /api/v1/variables-scoring";
                });
    }

    // Unauthenticated -> any protected endpoint -> 401
    @Test
    void unauthenticatedUserIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/roles/permisos"))
                .andExpect(status().isUnauthorized());
    }

    private String minimalSolicitantePayload() {
        return """
                {
                  "nombre": "Juan Carlos Perez",
                  "identificacion": "1017234567",
                  "fecha_nacimiento": "1990-05-15",
                  "ingresos_mensuales": 3500000,
                  "tipo_empleo": "Empleado",
                  "antiguedad_laboral": 36
                }
                """;
    }
}
