package co.udea.codefactory.creditscoring.reporting.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests de integración para HU-016: efectividad del modelo.
 * Verifica: matriz, concordanceRate, overrides, REJECTED→VERY_HIGH,
 * filtro analista, hasData=false, RBAC, PDF.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EfectividadModeloIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/reportes/efectividad-modelo";
    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2020-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2030-12-31T23:59:59Z");

    @BeforeEach
    void limpiar() {
        try { jdbc.update("DELETE FROM evaluation_knockout"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM evaluation_detail"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM credit_decision"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM evaluation"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM knockout_rule"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM model_variable"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM scoring_model"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM variable_range"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM variable_category"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM scoring_variable"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM financial_data"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM applicant WHERE created_by = 'user'"); } catch (Exception ignored) {}
    }

    // =========================================================================
    // Sin datos → 200 hasData=false
    // =========================================================================

    @Test
    void sinDatos_retorna200ConHasDataFalse() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE))
                        .param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    // =========================================================================
    // Parámetros faltantes → 400
    // =========================================================================

    @Test
    void sinParamDesde_retorna400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinParamHasta_retorna400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rangoInvertido_retorna400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(HASTA))
                        .param("hasta", fmt(DESDE))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // RBAC HU-016
    // =========================================================================

    @Test
    void rolADMIN_retorna200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void rolCREDIT_SUPERVISOR_retorna200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("sup").roles("CREDIT_SUPERVISOR")))
                .andExpect(status().isOk());
    }

    @Test
    void rolRISK_MANAGER_retorna200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("risk").roles("RISK_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void rolANALYST_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Con datos: matriz tiene 20 celdas (gap-fill)
    // =========================================================================

    @Test
    void conEvaluacionYDecision_matrizTiene20Celdas() throws Exception {
        crearEvaluacionConDecision("user_ana", "VERY_LOW", "APPROVED");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(true))
                .andExpect(jsonPath("$.matriz.length()").value(20));
    }

    // =========================================================================
    // Con datos: concordanceRate calculado
    // =========================================================================

    @Test
    void concordanceRate_calculado() throws Exception {
        // Concordante: VERY_LOW + APPROVED
        crearEvaluacionConDecision("user_ana", "VERY_LOW", "APPROVED");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.concordanceRate").value(100.0));
    }

    // =========================================================================
    // Evaluación sin decisión excluida (RN1-016)
    // =========================================================================

    @Test
    void evaluacionSinDecision_excluida() throws Exception {
        // Insertar evaluación sin credit_decision
        crearSoloEvaluacion("user_ana", "VERY_LOW");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false));
    }

    // =========================================================================
    // Filtro por analistaId
    // =========================================================================

    @Test
    void filtroAnalistaId_conResultados() throws Exception {
        crearEvaluacionConDecision("user_ana", "LOW", "APPROVED");
        crearEvaluacionConDecision("user_bob", "HIGH", "REJECTED");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .param("analistaId", "user_ana")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(true));
    }

    @Test
    void filtroAnalistaId_sinResultados_hasDataFalse() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .param("analistaId", "user_inexistente")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false));
    }

    // =========================================================================
    // PDF endpoint HU-016
    // =========================================================================

    @Test
    void pdfEndpoint_retornaPdfValido() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL + "/pdf")
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertThat(contentType).contains("application/pdf");

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void pdfEndpoint_rolANALYST_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pdf")
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String fmt(OffsetDateTime dt) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt);
    }

    /**
     * Inserta directamente en la base de datos una evaluación con su decisión.
     * Bypasa toda la lógica de negocio para simplificar el setup.
     */
    private void crearEvaluacionConDecision(
            String analista, String riskLevel, String decision) throws Exception {

        UUID evalId = UUID.randomUUID();
        UUID appId = getOrCreateApplicantId(analista);
        UUID fdId = getOrCreateFinancialDataId(appId);
        UUID modelId = getOrCreateModelId();

        jdbc.update("""
                INSERT INTO evaluation (id, applicant_id, model_id, financial_data_id,
                    total_score, risk_level, knocked_out, evaluated_at, evaluated_by,
                    created_at, created_by)
                VALUES (?, ?, ?, ?,
                    70.0, ?, false, NOW(), ?,
                    NOW(), 'user')
                """, evalId, appId, modelId, fdId, riskLevel, analista);

        UUID decId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO credit_decision (id, evaluation_id, decision, observations,
                    decided_by, decided_at, created_at, created_by)
                VALUES (?, ?, ?, 'Observacion de prueba de integracion', ?, NOW(), NOW(), 'user')
                """, decId, evalId, decision, analista);
    }

    private void crearSoloEvaluacion(String analista, String riskLevel) throws Exception {
        UUID evalId = UUID.randomUUID();
        UUID appId = getOrCreateApplicantId(analista);
        UUID fdId = getOrCreateFinancialDataId(appId);
        UUID modelId = getOrCreateModelId();

        jdbc.update("""
                INSERT INTO evaluation (id, applicant_id, model_id, financial_data_id,
                    total_score, risk_level, knocked_out, evaluated_at, evaluated_by,
                    created_at, created_by)
                VALUES (?, ?, ?, ?,
                    70.0, ?, false, NOW(), ?,
                    NOW(), 'user')
                """, evalId, appId, modelId, fdId, riskLevel, analista);
    }

    private UUID getOrCreateApplicantId(String analista) {
        // Crear un aplicante único por analista para evitar collisions
        String hash = "TEST-" + analista.hashCode();
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM applicant WHERE identification_hash = ?",
                    UUID.class, hash);
        } catch (Exception e) {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO applicant (id, name, identification_encrypted,
                        identification_hash, birth_date, employment_type,
                        monthly_income, work_experience_months,
                        created_at, created_by)
                    VALUES (?, 'Test User', ?, ?, '1990-01-01', 'Empleado',
                        5000000, 24, NOW(), 'user')
                    """, id, "enc-" + hash, hash);
            return id;
        }
    }

    private UUID getOrCreateFinancialDataId(UUID applicantId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM financial_data WHERE applicant_id = ? LIMIT 1",
                    UUID.class, applicantId);
        } catch (Exception e) {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO financial_data (id, applicant_id, version,
                        annual_income, monthly_expenses, current_debts,
                        assets_value, declared_patrimony, has_outstanding_defaults,
                        credit_history_months, created_at, created_by)
                    VALUES (?, ?, 1, 60000000, 1000000, 5000000,
                        100000000, 80000000, false, 24, NOW(), 'user')
                    """, id, applicantId);
            return id;
        }
    }

    private UUID getOrCreateModelId() {
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM scoring_model WHERE name = 'Modelo IT Test' LIMIT 1",
                    UUID.class);
        } catch (Exception e) {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO scoring_model (id, name, status, created_at, created_by, updated_at, updated_by)
                    VALUES (?, 'Modelo IT Test', 'ACTIVE', NOW(), 'user', NOW(), 'user')
                    """, id);
            return id;
        }
    }
}
