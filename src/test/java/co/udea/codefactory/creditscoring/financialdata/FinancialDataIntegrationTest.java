package co.udea.codefactory.creditscoring.financialdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class FinancialDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limpiar() {
        // financial_data referencia a applicant, por eso se borra primero
        jdbcTemplate.update("DELETE FROM financial_data");
        jdbcTemplate.update("DELETE FROM applicant");
    }

    // ==========================================================================
    // HU-004 — CA4: Se asocian al solicitante y se guarda la fecha de captura
    // ==========================================================================

    @Test
    void ca4_registrarDatosFinancieros_creaVersion1YRetorna201() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId").value(solicitanteId.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void ca4_registrarConSolicitanteInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes/" + UUID.randomUUID() + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isNotFound());
    }

    // ==========================================================================
    // HU-004 — CA1: Validación individual de campos (valores negativos → 400)
    // ==========================================================================

    @Test
    void ca1_registrarConActivosNegativos_retorna400() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        Map<String, Object> requestInvalido = requestFinancieroMap();
        requestInvalido.put("assetsValue", -100);

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ca3_registrarConMorasNegativas_retorna400() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        Map<String, Object> requestInvalido = requestFinancieroMap();
        requestInvalido.put("defaultsLast12m", -1);

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // HU-004 — CA2: Score de bureau es opcional; si no se ingresa → no disponible
    // ==========================================================================

    @Test
    void ca2_registrarSinScoreBureau_marcaComoNoDisponible() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        Map<String, Object> requestSinScore = requestFinancieroMap();
        requestSinScore.put("externalBureauScore", null);

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestSinScore)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalBureauScore").doesNotExist())
                .andExpect(jsonPath("$.externalBureauScoreAvailable").value(false));
    }

    // ==========================================================================
    // HU-004 — CA6: Resumen calculado incluye ratios
    // ==========================================================================

    @Test
    void ca6_registrarDatosFinancieros_retornaRatiosCalculados() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debtToIncomeRatio").isNumber())
                .andExpect(jsonPath("$.expenseToIncomeRatio").isNumber())
                .andExpect(jsonPath("$.debtToIncomeAlert").isBoolean())
                .andExpect(jsonPath("$.expensesExceedMonthlyIncome").isBoolean())
                .andExpect(jsonPath("$.liabilitiesExceedAssetsLimit").isBoolean());
    }

    // ==========================================================================
    // HU-004 — RN3: Alerta cuando ratio deuda/ingreso supera el 60%
    // ==========================================================================

    @Test
    void rn3_conRatioDeudaIngresaSobre60PorCiento_activaAlerta() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        // annualIncome = 10M, currentDebts = 7M → ratio = 70% > 60%
        Map<String, Object> requestAlto = requestFinancieroMap();
        requestAlto.put("annualIncome", 10000000);
        requestAlto.put("currentDebts", 7000000);

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAlto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debtToIncomeAlert").value(true));
    }

    // ==========================================================================
    // HU-004 — CA5: Actualización crea nueva versión histórica
    // ==========================================================================

    @Test
    void ca5_actualizarDatosFinancieros_creaVersionNueva() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // versión 1

        mockMvc.perform(put("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/1")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.applicantId").value(solicitanteId.toString()));
    }

    @Test
    void ca5_actualizarVersionInexistente_retorna404() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(put("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/99")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isNotFound());
    }

    // ==========================================================================
    // HU-004 — Autorización: solo ANALYST y ADMIN pueden registrar/actualizar
    // ==========================================================================

    @Test
    void autorizacion_riskManagerNoPoedRegistrar_retorna403() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("gestor").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isForbidden());
    }

    // ==========================================================================
    // HU-005 — CA1: Historial ordenado por versión descendente
    // ==========================================================================

    @Test
    void ca1_obtenerHistorial_retornaVersionesOrdenadasDescendente() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // versión 1
        actualizarDatosFinancieros(solicitanteId, 1); // versión 2

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[1].version").value(1));
    }

    @Test
    void ca1_obtenerHistorialDeSolicitanteSinDatos_retornaListaVacia() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ca1_obtenerHistorialDeSolicitanteInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/" + UUID.randomUUID() + "/datos-financieros")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isNotFound());
    }

    // ==========================================================================
    // HU-005 — CA2: Cada versión incluye campos y ratios calculados
    // ==========================================================================

    @Test
    void ca2_historialIncluyeRatiosPorCadaVersion() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId);

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debtToIncomeRatio").isNumber())
                .andExpect(jsonPath("$[0].expenseToIncomeRatio").isNumber())
                .andExpect(jsonPath("$[0].debtToIncomeAlert").isBoolean());
    }

    // ==========================================================================
    // HU-005 — CA3 + CA4: Comparación retorna campos modificados con estado
    // ==========================================================================

    @Test
    void ca3_compararDosVersiones_retornaAmbosConjuntosDeDatos() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // versión 1
        actualizarDatosFinancieros(solicitanteId, 1); // versión 2

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                        .param("v1", "1")
                        .param("v2", "2")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionBase.version").value(1))
                .andExpect(jsonPath("$.versionComparada.version").value(2))
                .andExpect(jsonPath("$.tendencia").isNotEmpty());
    }

    @Test
    void ca4_compararConDeudaMayor_clasificaComoDeterioro() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // versión 1: deuda = 5.000.000

        // Versión 2: deuda mayor (15.000.000)
        Map<String, Object> requestMayorDeuda = requestFinancieroMap();
        requestMayorDeuda.put("currentDebts", 15000000);
        mockMvc.perform(put("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/1")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMayorDeuda)))
                .andExpect(status().isOk());

        String json = mockMvc.perform(
                        get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                                .param("v1", "1").param("v2", "2")
                                .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tendencia").value("DETERIORO"))
                .andExpect(jsonPath("$.camposModificados").isArray())
                .andReturn().getResponse().getContentAsString();

        // El campo deuda_actual debe estar en la lista de campos modificados con estado DETERIORO
        assertThat(json).contains("\"campo\":\"deuda_actual\"").contains("\"estado\":\"DETERIORO\"");
    }

    @Test
    void ca4_compararConScoreMayor_clasificaComoMejora() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // versión 1: score = 720

        // Versión 2: score más alto (800) y menor deuda → tendencia MEJORA
        Map<String, Object> requestMejora = requestFinancieroMap();
        requestMejora.put("externalBureauScore", 800);
        requestMejora.put("currentDebts", 2000000); // menor deuda
        mockMvc.perform(put("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/1")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMejora)))
                .andExpect(status().isOk());

        String json = mockMvc.perform(
                        get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                                .param("v1", "1").param("v2", "2")
                                .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tendencia").value("MEJORA"))
                .andReturn().getResponse().getContentAsString();

        // El campo score_bureau debe estar en la lista con estado MEJORA
        assertThat(json).contains("\"campo\":\"score_bureau\"").contains("\"estado\":\"MEJORA\"");
    }

    // ==========================================================================
    // HU-005 — CA5 + RN1: Tendencia general
    // ==========================================================================

    @Test
    void ca5_rn1_tendenciaEstableCuandoRatioMejoraYScoreNoCambia() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // versión 1

        // Versión 2: menor deuda (ratio mejora) pero mismo score → ESTABLE por criterio RN1
        Map<String, Object> requestConMenorDeuda = requestFinancieroMap();
        requestConMenorDeuda.put("currentDebts", 1000000); // menos deuda
        // El score queda igual (720)
        mockMvc.perform(put("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/1")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestConMenorDeuda)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                        .param("v1", "1")
                        .param("v2", "2")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                // RN1: MEJORA requiere ratio↓ Y score↑; si solo ratio↓ → ESTABLE
                .andExpect(jsonPath("$.tendencia").value("ESTABLE"));
    }

    // ==========================================================================
    // HU-005 — Errores en comparación
    // ==========================================================================

    @Test
    void compararVersionesIguales_retorna400() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId);

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                        .param("v1", "1")
                        .param("v2", "1")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compararConVersionInexistente_retorna404() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId); // solo existe versión 1

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                        .param("v1", "1")
                        .param("v2", "99")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isNotFound());
    }

    // ==========================================================================
    // HU-005 — RN3: Autorización para consultar historial
    // ==========================================================================

    @Test
    void rn3_analystaAccedeAlHistorial_retorna200() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk());
    }

    @Test
    void rn3_riskManagerAccedeAlHistorial_retorna200() throws Exception {
        UUID solicitanteId = registrarSolicitante();

        mockMvc.perform(get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("gestor").roles("RISK_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void rn3_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/" + UUID.randomUUID() + "/datos-financieros"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================================================
    // HU-005 — El orden de los parámetros v1/v2 es irrelevante
    // ==========================================================================

    @Test
    void compararConOrdenInvertidoDeParametros_retornaElMismoResultado() throws Exception {
        UUID solicitanteId = registrarSolicitante();
        registrarDatosFinancieros(solicitanteId);
        actualizarDatosFinancieros(solicitanteId, 1);

        String respuestaOrdenNormal = mockMvc.perform(
                        get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                                .param("v1", "1").param("v2", "2")
                                .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String respuestaOrdenInvertido = mockMvc.perform(
                        get("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/comparar")
                                .param("v1", "2").param("v2", "1")
                                .with(user("analista").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuestaOrdenNormal).isEqualTo(respuestaOrdenInvertido);
    }

    // ==========================================================================
    // Helpers de test
    // ==========================================================================

    /** Registra un solicitante base y retorna su UUID. */
    @SuppressWarnings("unchecked")
    private UUID registrarSolicitante() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "nombre", "Ana García",
                "identificacion", "1017234567",
                "fecha_nacimiento", "1990-05-15",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 3500000,
                "antiguedad_laboral", 36));

        MvcResult resultado = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> respuesta = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), Map.class);
        // El campo de nivel raíz "id" contiene el UUID del solicitante registrado
        return UUID.fromString(respuesta.get("id").toString());
    }

    /** Registra la primera versión de datos financieros (POST). */
    private void registrarDatosFinancieros(UUID solicitanteId) throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isCreated());
    }

    /** Crea una nueva versión de datos financieros a partir de una versión existente (PUT). */
    private void actualizarDatosFinancieros(UUID solicitanteId, int version) throws Exception {
        mockMvc.perform(put("/api/v1/solicitantes/" + solicitanteId + "/datos-financieros/" + version)
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestFinanciero()))
                .andExpect(status().isOk());
    }

    /** JSON serializado del request financiero estándar. */
    private String requestFinanciero() throws Exception {
        return objectMapper.writeValueAsString(requestFinancieroMap());
    }

    /**
     * Mapa mutable con los valores de prueba estándar para datos financieros.
     * Se usa como mapa para permitir modificar campos individuales en tests específicos.
     */
    private java.util.HashMap<String, Object> requestFinancieroMap() {
        java.util.HashMap<String, Object> mapa = new java.util.HashMap<>();
        mapa.put("annualIncome", 36000000);
        mapa.put("monthlyExpenses", 2000000);
        mapa.put("currentDebts", 5000000);
        mapa.put("assetsValue", 20000000);
        mapa.put("declaredPatrimony", 15000000);
        mapa.put("hasOutstandingDefaults", false);
        mapa.put("creditHistoryMonths", 12);
        mapa.put("defaultsLast12m", 1);
        mapa.put("defaultsLast24m", 2);
        mapa.put("externalBureauScore", 720);
        mapa.put("activeCreditProducts", 3);
        return mapa;
    }
}
