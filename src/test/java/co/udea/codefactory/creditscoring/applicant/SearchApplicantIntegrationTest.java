package co.udea.codefactory.creditscoring.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * T-33/T-34 — SearchApplicantIntegrationTest
 * Covers AC-01 through AC-05 and role-based access for GET /api/v1/solicitantes
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SearchApplicantIntegrationTest {

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

    private void registerApplicant(String identification, String name) throws Exception {
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
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    // AC-01 — Búsqueda exitosa por número de identificación

    @Test
    void shouldFindApplicantByIdentification_returns200WithResults() throws Exception {
        registerApplicant("1017234567", "Juan Carlos Pérez");

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "1017234567")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldFindApplicantByIdentification_decryptsIdentification() throws Exception {
        registerApplicant("1017234567", "Juan Carlos Pérez");

        MvcResult result = mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "1017234567")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("1017234567");
    }

    // AC-02 — Búsqueda exitosa por nombre parcial

    @Test
    void shouldFindApplicantsByPartialName() throws Exception {
        registerApplicant("1001001001", "Carlos Pérez");
        registerApplicant("1002002002", "María Cardona");
        registerApplicant("1003003003", "Pedro González");

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "car")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    // AC-03 — Búsqueda case-insensitive

    @Test
    void shouldFindApplicantCaseInsensitively_lowercase() throws Exception {
        registerApplicant("1017234567", "JUAN PABLO RESTREPO");

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "juan pablo")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldFindApplicantCaseInsensitively_uppercase() throws Exception {
        registerApplicant("1017234567", "juan pablo restrepo");

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "JUAN PABLO")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // AC-04 — Búsqueda sin resultados

    @Test
    void shouldReturnEmptyContentWithMessageWhenNoResults() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "ZZZnombreInexistente")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldReturn200NotFound_whenNoResults() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "ZZZ999999")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk());
    }

    // AC-05 — Paginación

    @Test
    void shouldReturnPaginatedResults() throws Exception {
        for (int i = 1; i <= 25; i++) {
            registerApplicant("101723456" + String.format("%02d", i), "Solicitante Numero " + i);
        }

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void shouldReturnSecondPage() throws Exception {
        for (int i = 1; i <= 25; i++) {
            registerApplicant("101723456" + String.format("%02d", i), "Solicitante Numero " + i);
        }

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("page", "1")
                        .param("size", "20")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page.number").value(1));
    }

    // Role-based access

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200ForRiskManager() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("rm").roles("RISK_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200ForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200ForCreditSupervisor() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("cs").roles("CREDIT_SUPERVISOR")))
                .andExpect(status().isOk());
    }
}
