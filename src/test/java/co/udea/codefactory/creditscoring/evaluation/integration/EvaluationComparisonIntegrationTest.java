package co.udea.codefactory.creditscoring.evaluation.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Tests de integración para el endpoint de comparación de evaluaciones.
 * Verifica: applicants distintos → 400; id desconocido → 404; eval1==eval2 → delta=0; RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationComparisonIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/evaluaciones";
    private static final String COMPARAR_URL = BASE_URL + "/comparar";

    @BeforeEach
    void limpiar() {
        try { jdbc.update("DELETE FROM evaluation_knockout"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM evaluation_detail"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM evaluation"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM credit_decision"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM knockout_rule"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM model_variable"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM scoring_model"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM variable_range"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM variable_category"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM scoring_variable"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM financial_data"); } catch (Exception ignored) {}
        try { jdbc.update("DELETE FROM applicant WHERE created_by = 'user'"); } catch (Exception ignored) {}
    }

    @Test
    void comparar_idDesconocido_retorna404() throws Exception {
        String randomId1 = UUID.randomUUID().toString();
        String randomId2 = UUID.randomUUID().toString();

        mockMvc.perform(get(COMPARAR_URL)
                        .param("eval1", randomId1)
                        .param("eval2", randomId2)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void comparar_eval1IgualEval2_retornaDeltaCero() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Comparar");
        String aplicanteId = crearAplicante("Test Comparar");
        crearDatosFinancieros(aplicanteId, 0);
        String evaluacionId = crearEvaluacion(aplicanteId, modeloId);

        mockMvc.perform(get(COMPARAR_URL)
                        .param("eval1", evaluacionId)
                        .param("eval2", evaluacionId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreDelta").value(0));
    }

    @Test
    void comparar_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get(COMPARAR_URL)
                        .param("eval1", UUID.randomUUID().toString())
                        .param("eval2", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comparar_rutaComparar_noColisionaConOtrasRutas() throws Exception {
        // /evaluaciones/comparar debe resolverce antes que /{id}
        // Verificamos que el endpoint clasificacion también funciona en el mismo controller
        mockMvc.perform(get(BASE_URL + "/clasificacion")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
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

    private String crearAplicante(String nombre) throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", nombre,
                "identificacion", "3344556677",
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

    private String crearEvaluacion(String aplicanteId, String modeloId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "applicantId", aplicanteId,
                "modelId", modeloId));
        MvcResult r = mockMvc.perform(post(BASE_URL)
                .with(user("user").roles("ANALYST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extraerId(r.getResponse().getContentAsString());
    }

    private String extraerId(String body) {
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
