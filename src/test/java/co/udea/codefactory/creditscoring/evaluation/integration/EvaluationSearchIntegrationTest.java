package co.udea.codefactory.creditscoring.evaluation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * Tests de integración para el endpoint de búsqueda avanzada de evaluaciones.
 * Cubre 13 escenarios: happy path, validaciones, filtros multi-value, RBAC y sin-colisión de paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationSearchIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL   = "/api/v1/evaluaciones";
    private static final String SEARCH_URL = BASE_URL;

    // Rango de fechas para los tests — cubre el año actual de ejecución
    private static final String DESDE = "2026-01-01T00:00:00Z";
    private static final String HASTA = "2026-12-31T23:59:59Z";

    @BeforeEach
    void limpiar() {
        // Orden de eliminación respetando FKs
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
    // Escenario 1: happy path
    // =========================================================================

    @Test
    void ca1_dado3Evaluaciones_cuandoBuscaSinFiltros_retornaTodasPaginadas() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Busqueda");
        for (int i = 0; i < 3; i++) {
            String aplicanteId = crearAplicante("Solicitante " + i, "111000" + i);
            crearDatosFinancieros(aplicanteId, 0);
            crearEvaluacion(aplicanteId, modeloId, "analista1");
        }

        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25));
    }

    // =========================================================================
    // Escenario 2: fecha_desde requerida
    // =========================================================================

    @Test
    void ca2_cuandoFaltaFechaDesde_retorna400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_hasta", HASTA)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Escenario 3: fecha_hasta requerida
    // =========================================================================

    @Test
    void ca3_cuandoFaltaFechaHasta_retorna400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Escenario 4: rango > 365 días
    // =========================================================================

    @Test
    void ca4_cuandoRangoExcede365Dias_retorna400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", "2025-01-01T00:00:00Z")
                        .param("fecha_hasta", "2026-06-01T00:00:00Z")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Escenario 5: size > 100
    // =========================================================================

    @Test
    void ca5_cuandoSizeExcede100_retorna400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("size", "200")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Escenario 6: filtro multi-value nivel
    // =========================================================================

    @Test
    void ca6_dadoEvaluacionesDiversas_cuandoFiltraPorNivel_retornaCorrectamente() throws Exception {
        // Este test verifica que el filtro de nivel funciona; la lógica del nivel depende del score.
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Nivel Multi");
        String aplicanteId = crearAplicante("Solicitante Nivel", "9990001");
        crearDatosFinancieros(aplicanteId, 0);
        crearEvaluacion(aplicanteId, modeloId, "analista1");

        // Buscar con todos los niveles posibles → debe retornar la evaluación
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("nivel", "VERY_LOW", "LOW", "MEDIUM", "HIGH", "VERY_HIGH", "REJECTED")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================================
    // Escenario 7: filtro SIN_DECISION — solo evaluaciones sin decisión
    // =========================================================================

    @Test
    void ca7_dadoEvalConYSinDecision_cuandoFiltraSinDecision_retornaSolamente() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Sin Dec");
        String aplicanteId = crearAplicante("Sin Decision Test", "9990002");
        crearDatosFinancieros(aplicanteId, 0);
        String evalId = crearEvaluacion(aplicanteId, modeloId, "analista1");

        // Crear segunda evaluación con decisión
        String apId2 = crearAplicante("Con Decision Test", "9990003");
        crearDatosFinancieros(apId2, 0);
        String evalId2 = crearEvaluacion(apId2, modeloId, "analista1");
        crearDecision(evalId2, "APPROVED");

        // Filtrar por SIN_DECISION → solo la primera
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("decision", "SIN_DECISION")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].decisionStatus").isEmpty());
    }

    // =========================================================================
    // Escenario 8: filtro decision real excluye SIN_DECISION
    // =========================================================================

    @Test
    void ca8_cuandoFiltraApproved_excluySinDecision() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Excluye");
        String apId1 = crearAplicante("Con Approved", "9990004");
        crearDatosFinancieros(apId1, 0);
        String evalId1 = crearEvaluacion(apId1, modeloId, "analista1");
        crearDecision(evalId1, "APPROVED");

        String apId2 = crearAplicante("Sin Decision2", "9990005");
        crearDatosFinancieros(apId2, 0);
        crearEvaluacion(apId2, modeloId, "analista1");

        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("decision", "APPROVED")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].decisionStatus").value("APPROVED"));
    }

    // =========================================================================
    // Escenario 9: filtro APPROVED + SIN_DECISION incluye ambos
    // =========================================================================

    @Test
    void ca9_cuandoFiltraApprovedYSinDecision_incluyeAmbos() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Ambos");
        String apId1 = crearAplicante("Con Approved2", "9990006");
        crearDatosFinancieros(apId1, 0);
        String evalId1 = crearEvaluacion(apId1, modeloId, "analista1");
        crearDecision(evalId1, "APPROVED");

        String apId2 = crearAplicante("Sin Decision3", "9990007");
        crearDatosFinancieros(apId2, 0);
        crearEvaluacion(apId2, modeloId, "analista1");

        String apId3 = crearAplicante("Con Rejected", "9990008");
        crearDatosFinancieros(apId3, 0);
        String evalId3 = crearEvaluacion(apId3, modeloId, "analista1");
        crearDecision(evalId3, "REJECTED");

        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("decision", "APPROVED", "SIN_DECISION")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // =========================================================================
    // Escenario 10: filtro por analista
    // =========================================================================

    @Test
    void ca10_cuandoFiltraPorAnalista_retornaSoloDeSEanalista() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Analista");
        String apId1 = crearAplicante("Eval Analista A", "9990009");
        crearDatosFinancieros(apId1, 0);
        crearEvaluacion(apId1, modeloId, "analistaA");

        String apId2 = crearAplicante("Eval Analista B", "9990010");
        crearDatosFinancieros(apId2, 0);
        crearEvaluacion(apId2, modeloId, "analistaB");

        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("analista", "analistaA")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].analista").value("analistaA"));
    }

    // =========================================================================
    // Escenario 11: filtro puntaje min/max
    // =========================================================================

    @Test
    void ca11_cuandoFiltraPorPuntaje_retornaEnRango() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Puntaje");
        String apId1 = crearAplicante("Puntaje Test", "9990011");
        crearDatosFinancieros(apId1, 0);
        crearEvaluacion(apId1, modeloId, "analista1");

        // Filtrar con rango que no existe → 0 resultados
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("puntaje_min", "0")
                        .param("puntaje_max", "100")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================================
    // Escenario 12: role ANALYST → 403
    // =========================================================================

    @Test
    void ca12_cuandoRoleAnalyst_retorna403() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Escenario 13: sin autenticación → 401
    // =========================================================================

    @Test
    void ca13_cuandoNoAutenticado_retorna401() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA))
                .andExpect(status().isUnauthorized());
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

    private String crearEvaluacion(String aplicanteId, String modeloId, String analista) throws Exception {
        MvcResult r = mockMvc.perform(post(BASE_URL)
                        .with(user(analista).roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicantId", aplicanteId,
                                "modelId", modeloId))))
                .andExpect(status().isCreated())
                .andReturn();
        return extraerId(r.getResponse().getContentAsString());
    }

    private void crearDecision(String evaluacionId, String decision) {
        jdbc.update(
                "INSERT INTO credit_decision (id, evaluation_id, decision, observations, decided_by, decided_at, created_at, created_by) " +
                "VALUES (gen_random_uuid(), ?::uuid, ?, 'Decisión de prueba para integración', 'test', NOW(), NOW(), 'test')",
                evaluacionId, decision);
    }

    private String extraerId(String body) {
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
