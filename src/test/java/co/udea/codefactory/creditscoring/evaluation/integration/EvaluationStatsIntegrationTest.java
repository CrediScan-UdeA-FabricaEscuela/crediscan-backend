package co.udea.codefactory.creditscoring.evaluation.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Tests de integración para el endpoint de estadísticas de búsqueda de evaluaciones.
 * Cubre 3 escenarios: total/avg/distribution, filtros consistentes con search, RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationStatsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL  = "/api/v1/evaluaciones";
    private static final String STATS_URL = BASE_URL + "/estadisticas";

    // Rango de fechas para los tests — cubre el año actual de ejecución
    private static final String DESDE = "2026-01-01T00:00:00Z";
    private static final String HASTA = "2026-12-31T23:59:59Z";

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
    // Escenario 1: total, promedio y distribución correctos
    // =========================================================================

    @Test
    void ca1_dadoEvaluaciones_cuandoObteneStats_retornaTotalYPromedioCorrectos() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Stats");
        for (int i = 0; i < 3; i++) {
            String apId = crearAplicante("Stats Solicitante " + i, "8880" + i + "1");
            crearDatosFinancieros(apId, 0);
            crearEvaluacion(apId, modeloId, "analista1");
        }

        mockMvc.perform(get(STATS_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.averageScore").isNumber())
                .andExpect(jsonPath("$.distribution").isArray());
    }

    // =========================================================================
    // Escenario 2: estadísticas reflejan los mismos filtros que search
    // =========================================================================

    @Test
    void ca2_statsReflejanMismosFiltrosQueSearch() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Stats Filtros");
        String apId = crearAplicante("Stats Filtro", "8880021");
        crearDatosFinancieros(apId, 0);
        crearEvaluacion(apId, modeloId, "analista_stats");

        // Con filtro de analista que coincide → total 1
        mockMvc.perform(get(STATS_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("analista", "analista_stats")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        // Con filtro de analista que NO coincide → total 0
        mockMvc.perform(get(STATS_URL)
                        .param("fecha_desde", DESDE)
                        .param("fecha_hasta", HASTA)
                        .param("analista", "otro_analista")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    // =========================================================================
    // Escenario 3: role ANALYST → 403
    // =========================================================================

    @Test
    void ca3_cuandoRoleAnalyst_retorna403() throws Exception {
        mockMvc.perform(get(STATS_URL)
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
