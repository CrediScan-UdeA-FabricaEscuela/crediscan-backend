package co.udea.codefactory.creditscoring.reporting.integration;

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

/**
 * Tests de integración para el endpoint de distribución de riesgo.
 * Verifica: happy path, filtros, hasData=false, 400s y RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskDistributionReportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/reportes/distribucion-riesgo";

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

    // =========================================================================
    // Sin datos → 200 hasData=false
    // =========================================================================

    @Test
    void sinDatos_retorna200ConHasDataFalse() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void sinDatos_histogramaContieneExactamente10Bins() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.histograma").isArray())
                .andExpect(jsonPath("$.histograma.length()").value(10));
    }

    // =========================================================================
    // Happy path — con evaluaciones
    // =========================================================================

    @Test
    void conEvaluaciones_retorna200ConHasDataTrue() throws Exception {
        crearVariables();
        String modeloId = crearYActivarModelo("Modelo Reporte");
        String aplicanteId = crearAplicante("9900000001", "Empleado");
        crearDatosFinancieros(aplicanteId);
        crearEvaluacion(aplicanteId, modeloId);

        mockMvc.perform(get(BASE_URL)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(true))
                .andExpect(jsonPath("$.histograma.length()").value(10))
                .andExpect(jsonPath("$.tabla").isArray())
                .andExpect(jsonPath("$.overall").exists());
    }

    // =========================================================================
    // Filtro por tipo_empleo
    // =========================================================================

    @Test
    void filtroTipoEmpleo_validoSinDatos_retorna200HasDataFalse() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("tipo_empleo", "Pensionado")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(false));
    }

    @Test
    void filtroTipoEmpleo_invalido_retorna400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("tipo_empleo", "INVALIDO")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Rango invertido → 400
    // =========================================================================

    @Test
    void rangoInvertido_retorna400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("fecha_desde", "2025-06-01T00:00:00Z")
                        .param("fecha_hasta", "2025-01-01T00:00:00Z")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // RBAC
    // =========================================================================

    @Test
    void rolANALYST_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolRISK_MANAGER_retorna200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(user("gestor").roles("RISK_MANAGER")))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // PDF endpoint
    // =========================================================================

    @Test
    void pdfEndpoint_retorna200ConContentTypePdf() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL + "/pdf")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertThat(contentType).contains("application/pdf");

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void pdfEndpoint_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pdfEndpoint_rolANALYST_retorna403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pdf")
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

    private String crearAplicante(String identificacion, String tipoEmpleo) throws Exception {
        Map<String, Object> req = Map.of(
                "nombre", "Test Distribucion",
                "identificacion", identificacion,
                "fecha_nacimiento", "1990-01-01",
                "tipo_empleo", tipoEmpleo,
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

    private void crearDatosFinancieros(String aplicanteId) throws Exception {
        java.util.HashMap<String, Object> req = new java.util.HashMap<>();
        req.put("annualIncome", 60000000);
        req.put("monthlyExpenses", 1000000);
        req.put("currentDebts", 5000000);
        req.put("assetsValue", 100000000);
        req.put("declaredPatrimony", 80000000);
        req.put("hasOutstandingDefaults", false);
        req.put("creditHistoryMonths", 36);
        req.put("defaultsLast12m", 0);
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
        mockMvc.perform(post("/api/v1/evaluaciones")
                .with(user("user").roles("ANALYST"))
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
