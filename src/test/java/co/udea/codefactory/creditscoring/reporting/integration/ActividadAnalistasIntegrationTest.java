package co.udea.codefactory.creditscoring.reporting.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests de integración para HU-017: actividad de analistas.
 * Verifica: totales, distribución, tiempoMedio, outlier skipped,
 * hasData=false, RBAC, PDF, CSV.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActividadAnalistasIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private static final String BASE_URL = "/api/v1/reportes/actividad-analistas";
    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2020-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2030-12-31T23:59:59Z");

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM evaluation_knockout");
        jdbc.update("DELETE FROM evaluation_detail");
        jdbc.update("DELETE FROM credit_decision");
        jdbc.update("DELETE FROM evaluation");
        jdbc.update("DELETE FROM knockout_rule");
        jdbc.update("DELETE FROM model_variable");
        jdbc.update("DELETE FROM scoring_model");
        jdbc.update("DELETE FROM variable_range");
        jdbc.update("DELETE FROM variable_category");
        jdbc.update("DELETE FROM scoring_variable");
        jdbc.update("DELETE FROM financial_data");
        jdbc.update("DELETE FROM applicant WHERE created_by = 'user'");
    }

    // =========================================================================
    // Sin datos → 200 hasData=false
    // =========================================================================

    @Test
    void sinDatos_retorna200ConHasDataFalse() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
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
    void rangoInvertido_retorna400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(HASTA)).param("hasta", fmt(DESDE))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // RBAC HU-017 — RISK_MANAGER DENEGADO (boundary test)
    // =========================================================================

    @Test
    void rolCREDIT_SUPERVISOR_retorna200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("sup").roles("CREDIT_SUPERVISOR")))
                .andExpect(status().isOk());
    }

    @Test
    void rolRISK_MANAGER_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("risk").roles("RISK_MANAGER")))
                .andExpect(status().isForbidden());
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
    // Con datos: totales por analista
    // =========================================================================

    @Test
    void conEvaluacionYDecision_analistaAparece() throws Exception {
        crearEvaluacionConDecision("user_ana", "APPROVED");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(true))
                .andExpect(jsonPath("$.analistas.length()").value(1))
                .andExpect(jsonPath("$.analistas[0].totalEvaluaciones").value(1));
    }

    // =========================================================================
    // Evaluación sin decisión excluida
    // =========================================================================

    @Test
    void evaluacionSinDecision_excluida() throws Exception {
        crearSoloEvaluacion("user_ana");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false));
    }

    // =========================================================================
    // outlierDetectionSkipped=true cuando N < 3
    // =========================================================================

    @Test
    void unAnalista_outlierDetectionSkipped() throws Exception {
        crearEvaluacionConDecision("user_ana", "APPROVED");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadisticasEquipo.outlierDetectionSkipped").value(true));
    }

    // =========================================================================
    // Estadísticas del equipo: numAnalistas
    // =========================================================================

    @Test
    void dosAnalistas_estadisticasEquipoCorrectas() throws Exception {
        crearEvaluacionConDecision("user_ana", "APPROVED");
        crearEvaluacionConDecision("user_bob", "REJECTED");

        mockMvc.perform(get(BASE_URL)
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadisticasEquipo.numAnalistas").value(2))
                .andExpect(jsonPath("$.estadisticasEquipo.totalEvaluaciones").value(2));
    }

    // =========================================================================
    // PDF endpoint HU-017
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
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void pdfEndpoint_rolRISK_MANAGER_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pdf")
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("risk").roles("RISK_MANAGER")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // CSV endpoint HU-017
    // =========================================================================

    @Test
    void csvEndpoint_retornaCsvValido() throws Exception {
        crearEvaluacionConDecision("user_ana", "APPROVED");

        MvcResult result = mockMvc.perform(get(BASE_URL + "/csv")
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertThat(contentType).contains("text/csv");

        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains("analistaId");
        assertThat(csv).contains("totalEvaluaciones");
    }

    @Test
    void csvEndpoint_sinDatos_soloEncabezado() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL + "/csv")
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        // Solo la fila de encabezado cuando no hay datos
        String[] lineas = csv.trim().split("\n");
        assertThat(lineas).hasSize(1);
        assertThat(csv).contains("analistaId");
    }

    @Test
    void csvEndpoint_rolRISK_MANAGER_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/csv")
                        .param("desde", fmt(DESDE)).param("hasta", fmt(HASTA))
                        .with(user("risk").roles("RISK_MANAGER")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String fmt(OffsetDateTime dt) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt);
    }

    private void crearEvaluacionConDecision(String analista, String decision) {
        UUID evalId = UUID.randomUUID();
        UUID appId = getOrCreateApplicantId(analista);
        UUID fdId = getOrCreateFinancialDataId(appId);
        UUID modelId = getOrCreateModelId();

        jdbc.update("""
                INSERT INTO evaluation (id, applicant_id, model_id, financial_data_id,
                    total_score, risk_level, knocked_out, evaluated_at, evaluated_by,
                    created_at, created_by)
                VALUES (?, ?, ?, ?,
                    75.0, 'LOW', false, NOW(), ?,
                    NOW(), 'user')
                """, evalId, appId, modelId, fdId, analista);

        UUID decId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO credit_decision (id, evaluation_id, decision, observations,
                    decided_by, decided_at, created_at, created_by)
                VALUES (?, ?, ?, 'Observacion de prueba de integracion', ?, NOW(), NOW(), 'user')
                """, decId, evalId, decision, analista);
    }

    private void crearSoloEvaluacion(String analista) {
        UUID evalId = UUID.randomUUID();
        UUID appId = getOrCreateApplicantId(analista);
        UUID fdId = getOrCreateFinancialDataId(appId);
        UUID modelId = getOrCreateModelId();

        jdbc.update("""
                INSERT INTO evaluation (id, applicant_id, model_id, financial_data_id,
                    total_score, risk_level, knocked_out, evaluated_at, evaluated_by,
                    created_at, created_by)
                VALUES (?, ?, ?, ?,
                    75.0, 'LOW', false, NOW(), ?,
                    NOW(), 'user')
                """, evalId, appId, modelId, fdId, analista);
    }

    private UUID getOrCreateApplicantId(String analista) {
        String hash = "ACT-" + analista.hashCode();
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
                    "SELECT id FROM scoring_model WHERE name = 'Modelo IT Test Act' LIMIT 1",
                    UUID.class);
        } catch (Exception e) {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO scoring_model (id, name, status, created_at, created_by, updated_at, updated_by)
                    VALUES (?, 'Modelo IT Test Act', 'ACTIVE', NOW(), 'user', NOW(), 'user')
                    """, id);
            return id;
        }
    }
}
