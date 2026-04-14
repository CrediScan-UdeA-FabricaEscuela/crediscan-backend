package co.udea.codefactory.creditscoring.scoringmodel;

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
class ScoringModelIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String URL = "/api/v1/modelos-scoring";
    private static final String VAR_URL = "/api/v1/variables-scoring";

    @BeforeEach
    void limpiar() {
        // Limpiar en orden correcto por FK
        jdbc.update("DELETE FROM model_variable");
        jdbc.update("DELETE FROM scoring_model WHERE created_by = 'user'");
        jdbc.update("DELETE FROM variable_range WHERE variable_id IN (SELECT id FROM scoring_variable WHERE created_by = 'user')");
        jdbc.update("DELETE FROM variable_category WHERE variable_id IN (SELECT id FROM scoring_variable WHERE created_by = 'user')");
        jdbc.update("DELETE FROM scoring_variable WHERE created_by = 'user'");
    }

    // ==========================================================================
    // CA1: Crear modelo nuevo (desde cero — variables activas)
    // ==========================================================================

    @Test
    void ca1_crearModeloSinClonar_retorna201() throws Exception {
        crearVariableNumerica("VarA", 0.40);
        crearVariableNumerica("VarB", 0.35);
        crearVariableNumerica("VarC", 0.25);

        String json = objectMapper.writeValueAsString(Map.of("nombre", "Modelo inicial", "descripcion", "Primera versión"));

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Modelo inicial"))
                .andExpect(jsonPath("$.estado").value("DRAFT"))
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(jsonPath("$.variables").isArray());
    }

    @Test
    void ca1_crearModeloClonadoDesdeExistente_retorna201ConMismasVariables() throws Exception {
        crearVariableNumerica("VarClone1", 0.50);
        crearVariableNumerica("VarClone2", 0.50);
        String origenId = crearModelo("Modelo origen");

        String json = objectMapper.writeValueAsString(
                Map.of("nombre", "Clon del origen", "descripcion", "Clonado", "clonarDesde", origenId));

        MvcResult result = mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DRAFT"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // El clon debe tener las mismas variables que el origen
        assertThat(body).contains("variableId");
    }

    // ==========================================================================
    // CA2: Estado borrador, nombre descriptivo, fecha de creación
    // ==========================================================================

    @Test
    void ca2_modeloCreadoTieneEstadoDraft() throws Exception {
        crearVariableNumerica("VarCA2", 1.00);
        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", "Modelo CA2"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DRAFT"))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty());
    }

    // ==========================================================================
    // CA2: Listar todos los modelos
    // ==========================================================================

    @Test
    void ca2_listarModelos_retorna200() throws Exception {
        mockMvc.perform(get(URL).with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ==========================================================================
    // CA3/CA4: Solo un modelo activo a la vez; activar reemplaza al anterior
    // ==========================================================================

    @Test
    void ca3ca4_activarModelo_desactivaElAnterior() throws Exception {
        crearVariableNumerica("VarCA3-A", 0.40);
        crearVariableNumerica("VarCA3-B", 0.35);
        crearVariableNumerica("VarCA3-C", 0.25);

        // Crear y activar primer modelo
        String modelo1Id = crearModelo("Modelo v1 para CA3");
        activarModelo(modelo1Id);

        // Verificar que está activo
        mockMvc.perform(get(URL + "/" + modelo1Id).with(user("user").roles("ADMIN")))
                .andExpect(jsonPath("$.estado").value("ACTIVE"));

        // Crear segundo modelo y activar
        String modelo2Id = crearModelo("Modelo v2 para CA3");
        activarModelo(modelo2Id);

        // El primero debe ahora estar INACTIVE (CA4)
        mockMvc.perform(get(URL + "/" + modelo1Id).with(user("user").roles("ADMIN")))
                .andExpect(jsonPath("$.estado").value("INACTIVE"));

        // El segundo debe estar ACTIVE
        mockMvc.perform(get(URL + "/" + modelo2Id).with(user("user").roles("ADMIN")))
                .andExpect(jsonPath("$.estado").value("ACTIVE"));
    }

    // ==========================================================================
    // RN1: No se puede activar si suma de pesos ≠ 1.00
    // ==========================================================================

    @Test
    void rn1_activarConPesosSumandoDistintoDeUno_retorna400() throws Exception {
        // Dos variables con pesos que no suman 1.00
        crearVariableNumerica("RN1-VarA", 0.30);
        crearVariableNumerica("RN1-VarB", 0.40);
        crearVariableNumerica("RN1-VarC", 0.20); // suma = 0.90

        String modeloId = crearModelo("Modelo RN1");

        mockMvc.perform(put(URL + "/" + modeloId + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // RN2: No se puede activar con menos de 3 variables
    // ==========================================================================

    @Test
    void rn2_activarConMenosDeTresVariables_retorna400() throws Exception {
        crearVariableNumerica("RN2-VarA", 0.60);
        crearVariableNumerica("RN2-VarB", 0.40); // solo 2 variables

        String modeloId = crearModelo("Modelo RN2");

        mockMvc.perform(put(URL + "/" + modeloId + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // CA6: Comparar dos versiones
    // ==========================================================================

    @Test
    void ca6_compararDosVersiones_retorna200ConDiferencias() throws Exception {
        crearVariableNumerica("CompA", 0.40);
        crearVariableNumerica("CompB", 0.35);
        crearVariableNumerica("CompC", 0.25);

        String modelo1Id = crearModelo("Modelo Comp 1");
        String modelo2Id = crearModelo("Modelo Comp 2");

        mockMvc.perform(get(URL + "/comparar")
                        .param("base", modelo1Id)
                        .param("comparado", modelo2Id)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modeloBase.id").value(modelo1Id))
                .andExpect(jsonPath("$.modeloComparado.id").value(modelo2Id))
                .andExpect(jsonPath("$.diferencias").isArray());
    }

    // ==========================================================================
    // RBAC
    // ==========================================================================

    @Test
    void rbac_analistaNoCrear_retorna403() throws Exception {
        mockMvc.perform(post(URL)
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", "Intento"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rbac_nombre_duplicado_retorna400() throws Exception {
        crearVariableNumerica("VarDup", 1.00);
        crearModelo("Modelo duplicado");

        mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", "Modelo duplicado"))))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

    private void crearVariableNumerica(String nombre, double peso) throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", nombre, "descripcion", "Var de test", "tipo", "NUMERIC", "peso", peso,
                "rangos", List.of(
                        Map.of("limiteInferior", 0, "limiteSuperior", 5, "puntaje", 30, "etiqueta", "Bajo"),
                        Map.of("limiteInferior", 5, "limiteSuperior", 10, "puntaje", 70, "etiqueta", "Alto")),
                "categorias", List.of());
        mockMvc.perform(post(VAR_URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String crearModelo(String nombre) throws Exception {
        MvcResult result = mockMvc.perform(post(URL)
                        .with(user("user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private void activarModelo(String modeloId) throws Exception {
        mockMvc.perform(put(URL + "/" + modeloId + "/activar")
                        .with(user("user").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
