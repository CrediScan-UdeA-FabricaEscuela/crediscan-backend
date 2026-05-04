package co.udea.codefactory.creditscoring.scoringengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScoringEngineIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String MODELO_URL = "/api/v1/modelos-scoring";
    private static final String VAR_URL = "/api/v1/variables-scoring";
    private static final String CALCULAR_URL = "/api/v1/scoring/calcular";

    @BeforeEach
    void limpiar() {
        // TRUNCATE CASCADE para evitar problemas de FK
        jdbc.update("TRUNCATE TABLE evaluation RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE credit_decision RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE knockout_rule RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE model_variable RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE scoring_model RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE variable_range RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE variable_category RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE scoring_variable RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE financial_data RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE applicant RESTART IDENTITY CASCADE");
    }

    // ==========================================================================
    // CA4: CRUD de reglas knockout
    // ==========================================================================

    @Test
    void ca4_crearReglaKo_retorna201() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo KO");

        String body = objectMapper.writeValueAsString(Map.of(
                "campo", "moras_12_meses",
                "operador", "GT",
                "umbral", 3,
                "mensaje", "Más de 3 moras en los últimos 12 meses",
                "prioridad", 1));

        mockMvc.perform(post(MODELO_URL + "/" + modeloId + "/reglas-knockout")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.campo").value("moras_12_meses"))
                .andExpect(jsonPath("$.operador").value("GT"))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void ca4_listarReglasKo_retornaListaOrdenadaPorPrioridad() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo lista KO");
        crearReglaKo(modeloId, "moras_12_meses", "GT", 3, "Regla 1", 2);
        crearReglaKo(modeloId, "deudas_actuales", "GTE", 100000000, "Regla 2", 1);

        MvcResult result = mockMvc.perform(get(MODELO_URL + "/" + modeloId + "/reglas-knockout")
                        .with(user("user").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("moras_12_meses");
        assertThat(body).contains("deudas_actuales");
    }

    @Test
    void ca4_actualizarReglaKo_retorna200() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo update KO");
        String reglaId = crearReglaKo(modeloId, "moras_12_meses", "GT", 3, "Original", 1);

        String body = objectMapper.writeValueAsString(Map.of(
                "campo", "moras_12_meses",
                "operador", "GTE",
                "umbral", 4,
                "mensaje", "Actualizado",
                "prioridad", 1,
                "activa", true));

        mockMvc.perform(put(MODELO_URL + "/" + modeloId + "/reglas-knockout/" + reglaId)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operador").value("GTE"))
                .andExpect(jsonPath("$.umbral").value(4));
    }

    @Test
    void ca4_eliminarReglaKo_retorna204() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo delete KO");
        String reglaId = crearReglaKo(modeloId, "moras_12_meses", "GT", 3, "Para borrar", 1);

        mockMvc.perform(delete(MODELO_URL + "/" + modeloId + "/reglas-knockout/" + reglaId)
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isNoContent());

        // Verificar que ya no aparece en el listado
        MvcResult result = mockMvc.perform(get(MODELO_URL + "/" + modeloId + "/reglas-knockout")
                        .with(user("user").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(reglaId);
    }

    // ==========================================================================
    // CA3: Regla knockout rechaza antes de calcular
    // ==========================================================================

    @Test
    void ca3_koActivada_rechazaSinCalcularPuntaje() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo CA3");
        // Regla: moras_12_meses > 3 → rechazo
        crearReglaKo(modeloId, "moras_12_meses", "GT", 3, "Más de 3 moras", 1);

        // Solicitante con 5 moras
        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 5); // 5 moras > 3

        String body = objectMapper.writeValueAsString(Map.of(
                "aplicanteId", aplicanteId,
                "modeloId", modeloId));

        mockMvc.perform(post(CALCULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadoPorKo").value(true))
                .andExpect(jsonPath("$.mensajeKo").isNotEmpty())
                .andExpect(jsonPath("$.desglose").isEmpty());
    }

    // ==========================================================================
    // CA1: Cálculo correcto con promedio ponderado
    // ==========================================================================

    @Test
    void ca1_calculoPonderado_retornaPuntajeFinalConDesglose() throws Exception {
        // Tres variables con pesos que suman 1.00 (RN2: mínimo 3)
        crearVariableConNombre("moras_12_meses", 0.40);
        crearVariableConNombre("score_buro", 0.35);
        crearVariableConNombre("ingreso_anual", 0.25);
        String modeloId = crearYActivarModelo("Modelo CA1");

        String aplicanteId = crearAplicante();
        // 0 moras → rango [0,5) → puntaje 70
        // score_buro = 720 → rango [500,1000) → puntaje 70
        crearDatosFinancieros(aplicanteId, 0);

        String body = objectMapper.writeValueAsString(Map.of(
                "aplicanteId", aplicanteId,
                "modeloId", modeloId));

        mockMvc.perform(post(CALCULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadoPorKo").value(false))
                .andExpect(jsonPath("$.puntajeFinal").isNumber())
                .andExpect(jsonPath("$.desglose").isArray());
    }

    // ==========================================================================
    // CA6: Resultado incluye desglose y reglas evaluadas
    // ==========================================================================

    @Test
    void ca6_resultadoIncluyeDesgloseYReglasKoEvaluadas() throws Exception {
        crearVariableConNombre("moras_12_meses", 0.40);
        crearVariableConNombre("score_buro", 0.35);
        crearVariableConNombre("ingreso_anual", 0.25);
        String modeloId = crearYActivarModelo("Modelo CA6");
        crearReglaKo(modeloId, "moras_12_meses", "GT", 10, "KO test", 1);

        String aplicanteId = crearAplicante();
        crearDatosFinancieros(aplicanteId, 0); // 0 moras → KO no se activa

        String body = objectMapper.writeValueAsString(Map.of(
                "aplicanteId", aplicanteId,
                "modeloId", modeloId));

        mockMvc.perform(post(CALCULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose").isArray())
                .andExpect(jsonPath("$.reglasKoEvaluadas").isArray())
                .andExpect(jsonPath("$.reglasKoEvaluadas[0].activada").value(false));
    }

    // ==========================================================================
    // RBAC
    // ==========================================================================

    @Test
    void rbac_analistaNoCrearReglaKo_retorna403() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo RBAC");

        String body = objectMapper.writeValueAsString(Map.of(
                "campo", "moras_12_meses", "operador", "GT",
                "umbral", 3, "mensaje", "Test", "prioridad", 0));

        mockMvc.perform(post(MODELO_URL + "/" + modeloId + "/reglas-knockout")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

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
        String body = r.getResponse().getContentAsString();
        String modeloId = extraerId(body);

        mockMvc.perform(put(MODELO_URL + "/" + modeloId + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isOk());
        return modeloId;
    }

    private String crearReglaKo(String modeloId, String campo, String operador,
                                 long umbral, String mensaje, int prioridad) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "campo", campo, "operador", operador,
                "umbral", umbral, "mensaje", mensaje, "prioridad", prioridad));
        MvcResult r = mockMvc.perform(post(MODELO_URL + "/" + modeloId + "/reglas-knockout")
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extraerId(r.getResponse().getContentAsString());
    }

    private String crearAplicante() throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", "Juan Test",
                "identificacion", "1099887766",
                "fecha_nacimiento", "1990-01-01",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 4000000,
                "antiguedad_laboral", 24);
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
