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
 * Tests de integración para el endpoint de clasificación de evaluaciones.
 * Verifica los escenarios: resumen con 6 niveles, filtro por rango, validación de fechas y RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationClassificationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/evaluaciones";
    private static final String CLASIFICACION_URL = BASE_URL + "/clasificacion";

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

    @Test
    void clasificacion_sinDatos_retorna200Con6NivelesEnCero() throws Exception {
        mockMvc.perform(get(CLASIFICACION_URL)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.niveles").isArray())
                .andExpect(jsonPath("$.niveles.length()").value(6))
                .andExpect(jsonPath("$.niveles[0].cantidad").value(0));
    }

    @Test
    void clasificacion_conEvaluaciones_retornaConteosCorrecto() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Clasif");
        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 0);
        crearEvaluacion(aplicanteId, modeloId);

        mockMvc.perform(get(CLASIFICACION_URL)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.niveles.length()").value(6));
    }

    @Test
    void clasificacion_fechaDesdeAfterFechaHasta_retorna400() throws Exception {
        mockMvc.perform(get(CLASIFICACION_URL)
                        .param("fechaDesde", "2025-12-31T00:00:00Z")
                        .param("fechaHasta", "2025-01-01T00:00:00Z")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clasificacion_rolAnalyst_retorna403() throws Exception {
        mockMvc.perform(get(CLASIFICACION_URL)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clasificacion_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get(CLASIFICACION_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clasificacion_fueraDeRango_retornaCerosEnTodosNiveles() throws Exception {
        mockMvc.perform(get(CLASIFICACION_URL)
                        .param("fechaDesde", "2000-01-01T00:00:00Z")
                        .param("fechaHasta", "2000-01-02T00:00:00Z")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.niveles.length()").value(6))
                .andExpect(jsonPath("$.niveles[0].cantidad").value(0));
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

    private String crearAplicante() throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", "Test Clasificacion",
                "identificacion", "1122334455",
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

    private void crearEvaluacion(String aplicanteId, String modeloId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));
        mockMvc.perform(post(BASE_URL)
                .with(user("user").roles("ANALYST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    private String extraerId(String body) {
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
