package co.udea.codefactory.creditscoring.scoringengine;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScoringSimulationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String MODELO_URL = "/api/v1/modelos-scoring";
    private static final String VAR_URL = "/api/v1/variables-scoring";
    private static final String SIMULAR_URL = "/api/v1/scoring/simular";
    private static final String SIMULACIONES_URL = "/api/v1/scoring/simulaciones";

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM simulation_scenario");
        jdbc.update("DELETE FROM knockout_rule");
        jdbc.update("DELETE FROM model_variable");
        jdbc.update("DELETE FROM scoring_model WHERE created_by = 'user'");
        jdbc.update("DELETE FROM variable_range WHERE variable_id IN (SELECT id FROM scoring_variable WHERE created_by = 'user')");
        jdbc.update("DELETE FROM variable_category WHERE variable_id IN (SELECT id FROM scoring_variable WHERE created_by = 'user')");
        jdbc.update("DELETE FROM scoring_variable WHERE created_by = 'user'");
    }

    // ==========================================================================
    // CA1 — Simular con modelo ACTIVE
    // ==========================================================================

    @Test
    void ca1_simularConModeloActivo_retornaPuntajeSinPersistir() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Simul");

        Map<String, Object> body = Map.of(
                "modeloId", modeloId,
                "valoresVariables", Map.of(
                        "moras_12_meses", 0,
                        "score_buro", 720,
                        "ingreso_anual", 50000000));

        mockMvc.perform(post(SIMULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadoPorKo").value(false))
                .andExpect(jsonPath("$.puntajeFinal").isNumber())
                .andExpect(jsonPath("$.desglose").isArray());
    }

    // ==========================================================================
    // CA5 — Simular con modelo en DRAFT (modo prueba)
    // ==========================================================================

    @Test
    void ca5_simularConModeloDraft_retornaPuntaje() throws Exception {
        crearVariables();
        // Crear modelo pero NO activarlo — queda en DRAFT
        String modeloId = crearModelo("Modelo Draft");

        Map<String, Object> body = Map.of(
                "modeloId", modeloId,
                "valoresVariables", Map.of(
                        "moras_12_meses", 1,
                        "score_buro", 600,
                        "ingreso_anual", 30000000));

        mockMvc.perform(post(SIMULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadoPorKo").value(false))
                .andExpect(jsonPath("$.puntajeFinal").isNumber());
    }

    // ==========================================================================
    // CA3 — Simulación activa regla knockout
    // ==========================================================================

    @Test
    void ca3_simularConKoActivada_retornaRechazo() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo KO Simul");
        crearReglaKo(modeloId, "moras_12_meses", "GT", 3, "Más de 3 moras", 1);

        Map<String, Object> body = Map.of(
                "modeloId", modeloId,
                "valoresVariables", Map.of(
                        "moras_12_meses", 5,   // 5 > 3 → KO
                        "score_buro", 720,
                        "ingreso_anual", 50000000));

        mockMvc.perform(post(SIMULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadoPorKo").value(true))
                .andExpect(jsonPath("$.mensajeKo").isNotEmpty())
                .andExpect(jsonPath("$.desglose").isEmpty());
    }

    // ==========================================================================
    // CA6 — Guardar y listar escenarios
    // ==========================================================================

    @Test
    void ca6_guardarEscenario_retorna201YListadoContieneEscenario() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Guardar");

        Map<String, Object> body = Map.of(
                "modeloId", modeloId,
                "nombre", "Escenario Conservador",
                "descripcion", "Cliente con perfil bajo riesgo",
                "valoresVariables", Map.of(
                        "moras_12_meses", 0,
                        "score_buro", 750));

        MvcResult result = mockMvc.perform(post(SIMULACIONES_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Escenario Conservador"))
                .andExpect(jsonPath("$.modeloId").value(modeloId))
                .andReturn();

        // Verificar que aparece en el listado
        mockMvc.perform(get(SIMULACIONES_URL)
                        .with(user("user").roles("ANALYST"))
                        .param("modeloId", modeloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Escenario Conservador"));

        String bodyStr = result.getResponse().getContentAsString();
        assertThat(bodyStr).contains("Escenario Conservador");
    }

    // ==========================================================================
    // CA7 — Re-ejecutar escenario guardado
    // ==========================================================================

    @Test
    void ca7_ejecutarEscenarioGuardado_retornaPuntaje() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Ejecutar");

        // Guardar escenario
        Map<String, Object> saveBody = Map.of(
                "modeloId", modeloId,
                "nombre", "Escenario Reejecutar",
                "descripcion", "Para re-ejecutar",
                "valoresVariables", Map.of(
                        "moras_12_meses", 0,
                        "score_buro", 800,
                        "ingreso_anual", 60000000));

        MvcResult saveResult = mockMvc.perform(post(SIMULACIONES_URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String escenarioId = extraerId(saveResult.getResponse().getContentAsString());

        // Ejecutar el escenario guardado
        mockMvc.perform(post(SIMULACIONES_URL + "/" + escenarioId + "/ejecutar")
                        .with(user("user").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadoPorKo").value(false))
                .andExpect(jsonPath("$.puntajeFinal").isNumber());
    }

    // ==========================================================================
    // Validación — valores vacíos
    // ==========================================================================

    @Test
    void validacion_sinValoresVariables_retorna400() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Validacion");

        Map<String, Object> body = Map.of(
                "modeloId", modeloId,
                "valoresVariables", Map.of()); // vacío → inválido

        mockMvc.perform(post(SIMULAR_URL)
                        .with(user("user").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // RBAC — autenticación requerida
    // ==========================================================================

    @Test
    void rbac_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post(SIMULAR_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
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
                "descripcion", "Variable simulación " + nombre,
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

    private String crearModelo(String nombre) throws Exception {
        MvcResult r = mockMvc.perform(post(MODELO_URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        return extraerId(r.getResponse().getContentAsString());
    }

    private String crearYActivarModelo(String nombre) throws Exception {
        // El modelo se crea con las variables activas auto-incluidas (CA1 de HU-007)
        String modeloId = crearModelo(nombre);
        mockMvc.perform(put(MODELO_URL + "/" + modeloId + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isOk());
        return modeloId;
    }

    private void crearReglaKo(String modeloId, String campo, String operador,
                               long umbral, String mensaje, int prioridad) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "campo", campo, "operador", operador,
                "umbral", umbral, "mensaje", mensaje, "prioridad", prioridad));
        mockMvc.perform(post(MODELO_URL + "/" + modeloId + "/reglas-knockout")
                        .with(user("user").roles("ADMIN"))
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
