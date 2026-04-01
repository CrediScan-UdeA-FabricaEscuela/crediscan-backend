package co.udea.codefactory.creditscoring.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * T-35/T-36 — UpdateApplicantIntegrationTest
 * Covers AC-06 through AC-09 and role-based access for PATCH /api/v1/solicitantes/{id}
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UpdateApplicantIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM applicant_edit_audit");
        jdbcTemplate.update("DELETE FROM applicant");
    }

    private UUID registerApplicant(String identification, String name) throws Exception {
        String payload = """
                {
                  "nombre": "%s",
                  "identificacion": "%s",
                  "fecha_nacimiento": "1990-05-15",
                  "ingresos_mensuales": 3500000,
                  "tipo_empleo": "Empleado",
                  "antiguedad_laboral": 36
                }
                """.formatted(name, identification);

        MvcResult result = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return UUID.fromString(json.get("id").asText());
    }

    // AC-06 — Edición exitosa de teléfono con fila de auditoría

    @Test
    void shouldUpdatePhone_returns200WithCamposAuditados() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        String patchPayload = """
                { "telefono": "3001234567" }
                """;

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campos_auditados").isArray())
                .andExpect(jsonPath("$.campos_auditados[0]").value("telefono"))
                .andExpect(jsonPath("$.solicitante.telefono").value("3001234567"));
    }

    @Test
    void shouldUpdatePhone_createsAuditRow() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        String patchPayload = """
                { "telefono": "3001234567" }
                """;

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isOk());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applicant_edit_audit WHERE applicant_id = ? AND field_name = 'telefono'",
                Integer.class, id);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void shouldUpdatePhone_auditRowHasCorrectValues() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        String patchPayload = """
                { "telefono": "3001234567" }
                """;

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isOk());

        String newValue = jdbcTemplate.queryForObject(
                "SELECT new_value FROM applicant_edit_audit WHERE applicant_id = ? AND field_name = 'telefono'",
                String.class, id);
        assertThat(newValue).isEqualTo("3001234567");
    }

    // AC-07 — Rechazo de edición de identificación (campo inmutable)

    @Test
    void shouldReturn400WhenEditingIdentification() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        String patchPayload = """
                { "identificacion": "9999999999" }
                """;

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IMMUTABLE_FIELD"));
    }

    @Test
    void shouldReturn400WhenEditingBirthDate() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        String patchPayload = """
                { "fecha_nacimiento": "2000-01-01" }
                """;

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IMMUTABLE_FIELD"));
    }

    // AC-08 — Validación de ingresos negativos

    @Test
    void shouldReturn400WhenMonthlyIncomeIsNegative() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        String patchPayload = """
                { "ingresos_mensuales": -100000 }
                """;

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    // AC-09 — Acceso basado en roles para PATCH

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/solicitantes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForRiskManager() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("rm").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"telefono\": \"3001234567\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForCreditSupervisor() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("cs").roles("CREDIT_SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"telefono\": \"3001234567\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200ForAnalyst() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"telefono\": \"3001234567\" }"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200ForAdmin() throws Exception {
        UUID id = registerApplicant("1017234567", "Juan Carlos Pérez");

        mockMvc.perform(patch("/api/v1/solicitantes/" + id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"telefono\": \"3001234567\" }"))
                .andExpect(status().isOk());
    }

    // Additional — 404 for non-existent applicant

    @Test
    void shouldReturn404ForNonExistentApplicant() throws Exception {
        UUID nonExistent = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/solicitantes/" + nonExistent)
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"telefono\": \"3001234567\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
