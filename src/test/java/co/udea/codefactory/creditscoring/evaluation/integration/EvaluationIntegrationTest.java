package co.udea.codefactory.creditscoring.evaluation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
 * Tests de integración para el endpoint de evaluaciones crediticias.
 * Usa MockMvc + Testcontainers (PostgreSQL) para validar el flujo completo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String EVAL_URL = "/api/v1/evaluaciones";
    private static final String MODELO_URL = "/api/v1/modelos-scoring";
    private static final String VAR_URL = "/api/v1/variables-scoring";

    @BeforeEach
    void limpiar() {
        // Eliminar en orden correcto por FK — evaluaciones primero
        jdbc.update("DELETE FROM evaluation_knockout");
        jdbc.update("DELETE FROM evaluation_detail");
        jdbc.update("DELETE FROM evaluation");
        jdbc.update("DELETE FROM knockout_rule");
        jdbc.update("DELETE FROM model_variable");
        jdbc.update("DELETE FROM scoring_model WHERE created_by = 'user'");
        jdbc.update("DELETE FROM variable_range WHERE variable_id IN (SELECT id FROM scoring_variable WHERE created_by = 'user')");
        jdbc.update("DELETE FROM variable_category WHERE variable_id IN (SELECT id FROM scoring_variable WHERE created_by = 'user')");
        jdbc.update("DELETE FROM scoring_variable WHERE created_by = 'user'");
        jdbc.update("DELETE FROM financial_data");
        jdbc.update("DELETE FROM applicant WHERE created_by = 'user'");
    }

    // =========================================================================
    // Happy path: POST /api/v1/evaluaciones → 201
    // =========================================================================

    @Test
    void ca1_ejecutarEvaluacion_retorna201ConResultado() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Evaluacion");
        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 0);

        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));

        mockMvc.perform(post(EVAL_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.applicantId").value(aplicanteId))
                .andExpect(jsonPath("$.totalScore").isNumber())
                .andExpect(jsonPath("$.riskLevel").isNotEmpty())
                .andExpect(jsonPath("$.knockedOut").value(false))
                .andExpect(jsonPath("$.evaluatedBy").value("user"));
    }

    // =========================================================================
    // Cooldown activo: POST duplicado → 409
    // =========================================================================

    @Test
    void ca2_cooldownActivo_retorna409() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Cooldown");
        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 0);

        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));

        // Primera evaluación — debe ser exitosa
        mockMvc.perform(post(EVAL_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Segunda evaluación dentro del cooldown — debe ser rechazada
        mockMvc.perform(post(EVAL_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // Sin datos financieros: POST → 422
    // =========================================================================

    @Test
    void ca3_sinDatosFinancieros_retorna422() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Sin Financial");
        String aplicanteId = crearAplicante();
        // No se crean datos financieros para este solicitante

        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));

        mockMvc.perform(post(EVAL_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================================
    // Sin autenticación: POST → 401
    // =========================================================================

    @Test
    void rbac_sinAutenticacion_retorna401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", "00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post(EVAL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // RISK_MANAGER no puede crear evaluaciones: POST → 403
    // =========================================================================

    @Test
    void rbac_riskManagerNoCrearEvaluacion_retorna403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", "00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post(EVAL_URL)
                        .with(user("gestor").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /{id} existente → 200
    // =========================================================================

    @Test
    void ca4_obtenerEvaluacionExistente_retorna200() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Get");
        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 0);

        String postBody = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));

        MvcResult crearResult = mockMvc.perform(post(EVAL_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isCreated())
                .andReturn();

        String evaluacionId = extraerId(crearResult.getResponse().getContentAsString());

        mockMvc.perform(get(EVAL_URL + "/" + evaluacionId)
                        .with(user("user").roles("CREDIT_SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evaluacionId))
                .andExpect(jsonPath("$.applicantId").value(aplicanteId));
    }

    // =========================================================================
    // GET /{id} inexistente → 404
    // =========================================================================

    @Test
    void ca5_obtenerEvaluacionInexistente_retorna404() throws Exception {
        mockMvc.perform(get(EVAL_URL + "/00000000-0000-0000-0000-000000000099")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // GET /{id}/pdf → 200 + application/pdf
    // =========================================================================

    @Test
    void ca6_descargarPdf_retorna200ConPdf() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo PDF");
        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 0);

        String postBody = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));

        MvcResult crearResult = mockMvc.perform(post(EVAL_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isCreated())
                .andReturn();

        String evaluacionId = extraerId(crearResult.getResponse().getContentAsString());

        MvcResult pdfResult = mockMvc.perform(get(EVAL_URL + "/" + evaluacionId + "/pdf")
                        .with(user("user").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=evaluacion-" + evaluacionId + ".pdf"))
                .andReturn();

        // Verificar que el Content-Type sea application/pdf
        String contentType = pdfResult.getResponse().getContentType();
        assertThat(contentType).contains("application/pdf");

        // Verificar que el body tenga contenido (PDF no vacío)
        byte[] pdf = pdfResult.getResponse().getContentAsByteArray();
        assertThat(pdf.length).isGreaterThan(0);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void crearVariables() throws Exception {
        crearVariableConNombre("moras_12_meses", 0.40);
        crearVariableConNombre("score_buro", 0.35);
        crearVariableConNombre("ingreso_anual", 0.25);
    }

    private void crearVariableConNombre(String nombre, double peso) throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", nombre,
                "descripcion", "Variable de test para " + nombre,
                "tipo", "NUMERIC",
                "peso", peso,
                "rangos", List.of(
                        Map.of("limiteInferior", 0, "limiteSuperior", 5, "puntaje", 70, "etiqueta", "Bajo"),
                        Map.of("limiteInferior", 5, "limiteSuperior", 10000000000L, "puntaje", 30, "etiqueta", "Alto")),
                "categorias", List.of());
        mockMvc.perform(post(VAR_URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String crearYActivarModelo(String nombre) throws Exception {
        MvcResult r = mockMvc.perform(post(MODELO_URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        String modeloId = extraerId(r.getResponse().getContentAsString());

        mockMvc.perform(put(MODELO_URL + "/" + modeloId + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isOk());
        return modeloId;
    }

    private String crearAplicante() throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", "Test Evaluacion",
                "identificacion", "9988776655",
                "fecha_nacimiento", "1985-06-15",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 4000000,
                "antiguedad_laboral", 36);
        MvcResult r = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return extraerId(r.getResponse().getContentAsString());
    }

    private void crearDatosFinancieros(String aplicanteId, int moras12) throws Exception {
        java.util.HashMap<String, Object> req = new java.util.HashMap<>();
        req.put("annualIncome", 50000000);
        req.put("monthlyExpenses", 1000000);
        req.put("currentDebts", 5000000);
        req.put("assetsValue", 100000000);
        req.put("declaredPatrimony", 80000000);
        req.put("hasOutstandingDefaults", moras12 > 0);
        req.put("creditHistoryMonths", 36);
        req.put("defaultsLast12m", moras12);
        req.put("defaultsLast24m", 0);
        req.put("externalBureauScore", 720);
        req.put("activeCreditProducts", 2);
        mockMvc.perform(post("/api/v1/solicitantes/" + aplicanteId + "/datos-financieros")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String extraerId(String body) {
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
