package co.udea.codefactory.creditscoring.scoring;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScoringVariableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String URL = "/api/v1/variables-scoring";

    @BeforeEach
    void limpiar() {
        // TRUNCATE CASCADE para evitar problemas de FK
        jdbcTemplate.update("TRUNCATE TABLE evaluation RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE credit_decision RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE knockout_rule RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE model_variable RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE scoring_model RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE variable_range RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE variable_category RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE scoring_variable RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE financial_data RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE applicant RESTART IDENTITY CASCADE");
    }

    // ==========================================================================
    // CA1: Crear variable numérica con rangos válidos → 201
    // ==========================================================================

    @Test
    void ca1_crearVariableNumerica_retorna201ConId() throws Exception {
        String json = objectMapper.writeValueAsString(requestNumerico("Antigüedad laboral", 0.30));

        MvcResult result = mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Antigüedad laboral"))
                .andExpect(jsonPath("$.tipo").value("NUMERIC"))
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.rangos").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"limiteInferior\"");
    }

    @Test
    void ca1_crearVariableCategorica_retorna201ConCategorias() throws Exception {
        String json = objectMapper.writeValueAsString(requestCategorico("Tipo de empleo", 0.20));

        mockMvc.perform(post(URL)
                        .with(user("user").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CATEGORICAL"))
                .andExpect(jsonPath("$.categorias").isArray());
    }

    // ==========================================================================
    // CA2: Nombre duplicado → 400
    // ==========================================================================

    @Test
    void ca2_crearConNombreDuplicado_retorna400() throws Exception {
        String json = objectMapper.writeValueAsString(requestNumerico("Variable única", 0.30));

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Segunda creación con mismo nombre
        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // CA3: Actualizar variable existente → 200
    // ==========================================================================

    @Test
    void ca3_actualizarVariable_retorna200ConDatosNuevos() throws Exception {
        String idCreada = crearVariableNumerica("Variable para actualizar", 0.30);

        Map<String, Object> update = Map.of(
                "nombre", "Variable para actualizar",
                "descripcion", "Descripción actualizada",
                "peso", 0.40,
                "activa", true,
                "rangos", rangoRequestList(),
                "categorias", List.of());

        mockMvc.perform(put(URL + "/" + idCreada)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idCreada))
                .andExpect(jsonPath("$.descripcion").value("Descripción actualizada"))
                .andExpect(jsonPath("$.peso").value(0.40));
    }

    @Test
    void ca3_actualizarConIdInexistente_retorna404() throws Exception {
        Map<String, Object> update = Map.of(
                "nombre", "No existe",
                "descripcion", "Desc",
                "peso", 0.30,
                "activa", true,
                "rangos", rangoRequestList(),
                "categorias", List.of());

        mockMvc.perform(put(URL + "/" + UUID.randomUUID())
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // ==========================================================================
    // CA5: Rangos no contiguos → 400
    // ==========================================================================

    @Test
    void ca5_rangosConGap_retorna400() throws Exception {
        Map<String, Object> request = Map.of(
                "nombre", "Variable con gap",
                "descripcion", "Desc",
                "tipo", "NUMERIC",
                "peso", 0.20,
                "rangos", List.of(
                        Map.of("limiteInferior", 0, "limiteSuperior", 5, "puntaje", 30),
                        // Gap: falta el tramo 5-6
                        Map.of("limiteInferior", 6, "limiteSuperior", 10, "puntaje", 70)),
                "categorias", List.of());

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ca5_primerRangoNoEmpienzaEn0_retorna400() throws Exception {
        Map<String, Object> request = Map.of(
                "nombre", "Variable sin cero",
                "descripcion", "Desc",
                "tipo", "NUMERIC",
                "peso", 0.20,
                "rangos", List.of(
                        Map.of("limiteInferior", 1, "limiteSuperior", 10, "puntaje", 50)),
                "categorias", List.of());

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // CA6: Variable categórica sin categorías → 400
    // ==========================================================================

    @Test
    void ca6_variableCategoricaSinCategorias_retorna400() throws Exception {
        Map<String, Object> request = Map.of(
                "nombre", "Tipo empleo vacío",
                "descripcion", "Desc",
                "tipo", "CATEGORICAL",
                "peso", 0.20,
                "rangos", List.of(),
                "categorias", List.of());

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // CA7: Listar variables con suma de pesos y advertencias
    // ==========================================================================

    @Test
    void ca7_listarVariables_retorna200ConSumaPesosYAdvertencias() throws Exception {
        crearVariableNumerica("Variable A", 0.30);
        crearVariableNumerica("Variable B", 0.20);

        MvcResult result = mockMvc.perform(get(URL)
                        .with(user("user").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables").isArray())
                .andExpect(jsonPath("$.sumaPesos").isNumber())
                .andExpect(jsonPath("$.advertencias").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Hay menos de 3 variables activas → debe advertir por RN4
        assertThat(body).contains("RN4");
    }

    @Test
    void ca7_listarConSumaPesosDiferenteDeUno_incluyeAdvertenciaCA4() throws Exception {
        // Peso total = 0.30 + 0.40 = 0.70 (≠ 1.00)
        crearVariableNumerica("Var CA4-A", 0.30);
        crearVariableNumerica("Var CA4-B", 0.40);

        MvcResult result = mockMvc.perform(get(URL)
                        .with(user("user").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("CA4");
    }

    // ==========================================================================
    // RBAC: Solo ADMIN y RISK_MANAGER pueden crear/actualizar
    // ==========================================================================

    @Test
    void rbac_analista_noPuedeCrear_retorna403() throws Exception {
        String json = objectMapper.writeValueAsString(requestNumerico("Variable analista", 0.30));

        mockMvc.perform(post(URL)
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void rbac_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestNumerico("Sin auth", 0.30))))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================================================
    // Peso: validaciones básicas
    // ==========================================================================

    @Test
    void peso_menorQueMinimo_retorna400() throws Exception {
        Map<String, Object> request = Map.of(
                "nombre", "Peso inválido",
                "descripcion", "Desc",
                "tipo", "NUMERIC",
                "peso", 0.005,
                "rangos", rangoRequestList(),
                "categorias", List.of());

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private String crearVariableNumerica(String nombre, double peso) throws Exception {
        String json = objectMapper.writeValueAsString(requestNumerico(nombre, peso));
        MvcResult result = mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // Extraer el id del JSON
        int idStart = body.indexOf("\"id\":\"") + 6;
        int idEnd = body.indexOf("\"", idStart);
        return body.substring(idStart, idEnd);
    }

    private Map<String, Object> requestNumerico(String nombre, double peso) {
        return Map.of(
                "nombre", nombre,
                "descripcion", "Variable numérica de prueba",
                "tipo", "NUMERIC",
                "peso", peso,
                "rangos", rangoRequestList(),
                "categorias", List.of());
    }

    private Map<String, Object> requestCategorico(String nombre, double peso) {
        return Map.of(
                "nombre", nombre,
                "descripcion", "Variable categórica de prueba",
                "tipo", "CATEGORICAL",
                "peso", peso,
                "rangos", List.of(),
                "categorias", List.of(
                        Map.of("categoria", "Empleado", "puntaje", 80),
                        Map.of("categoria", "Independiente", "puntaje", 50)));
    }

    private List<Map<String, Object>> rangoRequestList() {
        return List.of(
                Map.of("limiteInferior", 0, "limiteSuperior", 5, "puntaje", 30, "etiqueta", "Bajo"),
                Map.of("limiteInferior", 5, "limiteSuperior", 10, "puntaje", 70, "etiqueta", "Alto"));
    }
}
