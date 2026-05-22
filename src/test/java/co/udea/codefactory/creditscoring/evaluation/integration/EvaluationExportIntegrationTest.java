package co.udea.codefactory.creditscoring.evaluation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
 * Tests de integración para el endpoint de exportación de evaluaciones (CSV y PDF).
 * Cubre 5 escenarios: CSV happy path, CSV SIN_DECISION, PDF happy path, PDF > 1000 → 422, RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationExportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL   = "/api/v1/evaluaciones";
    private static final String EXPORT_URL = BASE_URL + "/export";

    // Rango de fechas para los tests — cubre el año actual de ejecución
    private static final String DESDE = "2026-01-01T00:00:00Z";
    private static final String HASTA = "2026-12-31T23:59:59Z";

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
    // Escenario 1: CSV happy path — Content-Type + headers + rows
    // =========================================================================

    @Test
    void ca1_csvHappyPath_contentTypeCsvYContieneDatos() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo CSV");
        String apId = crearAplicante("Solicitante CSV", "7770001");
        crearDatosFinancieros(apId, 0);
        crearEvaluacion(apId, modeloId, "analista1");

        MvcResult result = mockMvc.perform(get(EXPORT_URL)
                        .param("formato", "CSV")
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertThat(contentType).contains("text/csv");

        String csv = result.getResponse().getContentAsString();
        // Verificar headers del CSV
        assertThat(csv).contains("solicitante");
        assertThat(csv).contains("fecha");
        assertThat(csv).contains("puntaje");
        assertThat(csv).contains("nivel");
        assertThat(csv).contains("decision");
        assertThat(csv).contains("analista");
        // Verificar que hay al menos 1 fila de datos
        assertThat(csv.split("\n").length).isGreaterThanOrEqualTo(2);
    }

    // =========================================================================
    // Escenario 2: CSV con SIN_DECISION se renderiza en columna decision
    // =========================================================================

    @Test
    void ca2_csvSinDecision_renderizaComoSinDecisionEnColumnaDecision() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo CSV Sin Dec");
        String apId = crearAplicante("Sin Dec CSV", "7770002");
        crearDatosFinancieros(apId, 0);
        crearEvaluacion(apId, modeloId, "analista1");
        // No creamos credit_decision → la evaluación queda SIN_DECISION

        MvcResult result = mockMvc.perform(get(EXPORT_URL)
                        .param("formato", "CSV")
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("SIN_DECISION");
    }

    // =========================================================================
    // Escenario 3: PDF happy path — magic bytes %PDF
    // =========================================================================

    @Test
    void ca3_pdfHappyPath_contentTypePdfYMagicBytes() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo PDF Export");
        String apId = crearAplicante("Solicitante PDF", "7770003");
        crearDatosFinancieros(apId, 0);
        crearEvaluacion(apId, modeloId, "analista1");

        MvcResult result = mockMvc.perform(get(EXPORT_URL)
                        .param("formato", "PDF")
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertThat(contentType).contains("application/pdf");

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }

    // =========================================================================
    // Escenario 4: PDF con count > 1000 → 422
    // (Simulamos insertando 1001 evaluaciones directamente en BD)
    // =========================================================================

    @Test
    void ca4_pdfConCountMayor1000_retorna422() throws Exception {
        // Crear solicitante y datos financieros base para las inserciones bulk
        String modelId = crearModeloDirecto();
        String applicantId = crearAplicanteDirecto();
        String financialDataId = crearFinancialDataDirecto(applicantId);

        // Insertar 1001 evaluaciones directamente para sobrepasar el límite del PDF
        for (int i = 0; i < 1001; i++) {
            jdbc.update(
                "INSERT INTO evaluation (id, applicant_id, model_id, financial_data_id, " +
                "total_score, risk_level, knocked_out, evaluated_at, evaluated_by, created_at, created_by) " +
                "VALUES (gen_random_uuid(), ?::uuid, ?::uuid, ?::uuid, " +
                "75.0, 'LOW', false, '2026-06-01T00:00:00Z'::timestamptz, 'analista_bulk', NOW(), 'test')",
                applicantId, modelId, financialDataId);
        }

        mockMvc.perform(get(EXPORT_URL)
                        .param("formato", "PDF")
                        .param("fecha_desde", "2026-05-01T00:00:00Z")
                        .param("fecha_hasta", "2026-07-31T23:59:59Z")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================================
    // Escenario 5: role ANALYST → 403
    // =========================================================================

    @Test
    void ca5_cuandoRoleAnalyst_retorna403() throws Exception {
        mockMvc.perform(get(EXPORT_URL)
                        .param("formato", "CSV")
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void crearVariables() throws Exception {
        crearVariable("moras_12_meses", 0.40);
        crearVariable("score_buro", 0.35);
        crearVariable("ingreso_anual", 0.25);
    }

    private void crearVariable(String nombre, double peso) throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", nombre,
                "descripcion", "Variable de test",
                "tipo", "NUMERIC",
                "peso", peso,
                "rangos", List.of(
                        Map.of("limiteInferior", 0, "limiteSuperior", 5, "puntaje", 70, "etiqueta", "Bajo"),
                        Map.of("limiteInferior", 5, "limiteSuperior", 10000000000L, "puntaje", 30, "etiqueta", "Alto")),
                "categorias", List.of());
        mockMvc.perform(post("/api/v1/variables-scoring")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String crearYActivarModelo(String nombre) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/modelos-scoring")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        String id = extraerId(r.getResponse().getContentAsString());
        mockMvc.perform(put("/api/v1/modelos-scoring/" + id + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isOk());
        return id;
    }

    /** Crea un modelo de scoring directamente en BD para el test de bulk. */
    private String crearModeloDirecto() {
        String modelId = java.util.UUID.randomUUID().toString();
        jdbc.update(
            "INSERT INTO scoring_model (id, name, status, min_score, max_score, version, created_at, created_by) " +
            "VALUES (?::uuid, 'Modelo Bulk', 'ACTIVE', 0, 100, 1, NOW(), 'test')",
            modelId);
        return modelId;
    }

    /** Crea un aplicante directamente en BD para el test de bulk. */
    private String crearAplicanteDirecto() {
        String applicantId = java.util.UUID.randomUUID().toString();
        jdbc.update(
            "INSERT INTO applicant (id, name, identification_encrypted, identification_hash, " +
            "birth_date, employment_type, monthly_income, work_experience_months, created_at, created_by) " +
            "VALUES (?::uuid, 'Bulk Test', 'enc_bulk_' || ?, 'hash_bulk_' || ?, " +
            "'1990-01-01', 'Empleado', 5000000, 24, NOW(), 'test')",
            applicantId, applicantId, applicantId);
        return applicantId;
    }

    /** Crea financial_data directamente en BD para el test de bulk. */
    private String crearFinancialDataDirecto(String applicantId) {
        String fdId = java.util.UUID.randomUUID().toString();
        jdbc.update(
            "INSERT INTO financial_data (id, applicant_id, version, annual_income, monthly_expenses, " +
            "current_debts, assets_value, declared_patrimony, has_outstanding_defaults, " +
            "credit_history_months, created_at, created_by) " +
            "VALUES (?::uuid, ?::uuid, 1, 60000000, 1000000, 5000000, 100000000, 80000000, " +
            "false, 36, NOW(), 'test')",
            fdId, applicantId);
        return fdId;
    }

    private String crearAplicante(String nombre, String identificacion) throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", nombre,
                "identificacion", identificacion,
                "fecha_nacimiento", "1990-01-01",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 5000000,
                "antiguedad_laboral", 24);
        MvcResult r = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return extraerId(r.getResponse().getContentAsString());
    }

    private void crearDatosFinancieros(String aplicanteId, int moras) throws Exception {
        java.util.HashMap<String, Object> req = new java.util.HashMap<>();
        req.put("annualIncome", 60000000);
        req.put("monthlyExpenses", 1000000);
        req.put("currentDebts", 5000000);
        req.put("assetsValue", 100000000);
        req.put("declaredPatrimony", 80000000);
        req.put("hasOutstandingDefaults", moras > 0);
        req.put("creditHistoryMonths", 36);
        req.put("defaultsLast12m", moras);
        req.put("defaultsLast24m", 0);
        req.put("externalBureauScore", 720);
        req.put("activeCreditProducts", 2);
        mockMvc.perform(post("/api/v1/solicitantes/" + aplicanteId + "/datos-financieros")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private void crearEvaluacion(String aplicanteId, String modeloId, String analista) throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(analista).roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicantId", aplicanteId,
                                "modelId", modeloId))))
                .andExpect(status().isCreated());
    }

    private String extraerId(String body) {
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
