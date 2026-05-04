package co.udea.codefactory.creditscoring.creditdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class CreditDecisionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID evaluationId;
    private String applicantId;
    private UUID financialDataId;
    private UUID modelId;

    @BeforeEach
    void setUp() {
        // Limpiar tablas en orden correcto para no violar FK
        // Usamos TRUNCATE CASCADE para evitar FK violations
        // Solo truncamos tablas que existen en el schema de test
        try { jdbcTemplate.update("TRUNCATE TABLE credit_decision CASCADE"); } catch (Exception ignored) {}
        try { jdbcTemplate.update("TRUNCATE TABLE evaluation CASCADE"); } catch (Exception ignored) {}
        try { jdbcTemplate.update("TRUNCATE TABLE financial_data CASCADE"); } catch (Exception ignored) {}
        try { jdbcTemplate.update("TRUNCATE TABLE scoring_model CASCADE"); } catch (Exception ignored) {}
        try { jdbcTemplate.update("TRUNCATE TABLE scoring_variable CASCADE"); } catch (Exception ignored) {}
        try { jdbcTemplate.update("TRUNCATE TABLE applicant CASCADE"); } catch (Exception ignored) {}

        // Crear datos base
        try {
            crearDatosBase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test data", e);
        }
    }

    private void crearDatosBase() throws Exception {
        // 1. Crear solicitante
        String applicantJson = objectMapper.writeValueAsString(Map.of(
                "nombre", "Juan Pérez",
                "identificacion", "1017234568",
                "fecha_nacimiento", "1990-05-15",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 3500000,
                "antiguedad_laboral", 36));

        MvcResult applicantResult = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicantJson))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> applicantMap = objectMapper.readValue(
                applicantResult.getResponse().getContentAsString(), Map.class);
        applicantId = applicantMap.get("id").toString();

        // 2. Crear modelo de scoring
        String modelJson = objectMapper.writeValueAsString(Map.of(
                "nombre", "Modelo Test",
                "descripcion", "Modelo de prueba"));

        MvcResult modelResult = mockMvc.perform(post("/api/v1/modelos-scoring")
                        .with(user("analista").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modelJson))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> modelMap = objectMapper.readValue(
                modelResult.getResponse().getContentAsString(), Map.class);
        modelId = UUID.fromString(modelMap.get("id").toString());

        // 2b. Registrar regla knockout para el modelo (moras >= 5 → rechazo automático)
        mockMvc.perform(post("/api/v1/modelos-scoring/" + modelId + "/reglas-knockout")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "campo", "moras_12_meses",
                                "operador", "GTE",
                                "umbral", 5,
                                "mensaje", "Más de 5 moras en los últimos 12 meses: rechazo automático",
                                "prioridad", 1))))
                .andExpect(status().isCreated());

        // 3. Registrar variable de scoring
        mockMvc.perform(post("/api/v1/variables-scoring")
                        .with(user("analista").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "moras_12_meses",
                                "descripcion", "Var test",
                                "tipo", "NUMERIC",
                                "peso", 1.0,
                                "rangos", java.util.List.of(
                                        Map.of("limiteInferior", 0, "limiteSuperior", 5, "puntaje", 70, "etiqueta", "Bajo"),
                                        Map.of("limiteInferior", 5, "limiteSuperior", 100, "puntaje", 30, "etiqueta", "Alto")),
                                "categorias", java.util.List.of()))))
                .andExpect(status().isCreated());

        // 4. Registrar datos financieros
        java.util.HashMap<String, Object> financialMap = new java.util.HashMap<>();
        financialMap.put("annualIncome", 36000000);
        financialMap.put("monthlyExpenses", 2000000);
        financialMap.put("currentDebts", 5000000);
        financialMap.put("assetsValue", 20000000);
        financialMap.put("declaredPatrimony", 15000000);
        financialMap.put("hasOutstandingDefaults", false);
        financialMap.put("creditHistoryMonths", 12);
        financialMap.put("defaultsLast12m", 0);
        financialMap.put("defaultsLast24m", 0);
        financialMap.put("externalBureauScore", 720);
        financialMap.put("activeCreditProducts", 3);

        mockMvc.perform(post("/api/v1/solicitantes/" + applicantId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financialMap)))
                .andExpect(status().isCreated());

        // 5. Ejecutar evaluación
        String evalJson = objectMapper.writeValueAsString(Map.of(
                "applicantId", applicantId,
                "modelId", modelId.toString()));

        MvcResult evalResult = mockMvc.perform(post("/api/v1/evaluaciones")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evalJson))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> evalMap = objectMapper.readValue(
                evalResult.getResponse().getContentAsString(), Map.class);
        evaluationId = UUID.fromString(evalMap.get("id").toString());
    }

    private String requestDecision(String decision) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "decision", decision,
                "observations", "El solicitante cumple todos los criterios de riesgo crediticio"));
    }

    // ==========================================================================
    // CA1 — Solo una decisión por evaluación
    // ==========================================================================

    @Test
    @DisplayName("CA1: registrar decisión en evaluación sin decisión retorna 201")
    void ca1_registrarDecisionEnEvaluacionSinDecision_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.evaluationId").value(evaluationId.toString()))
                .andExpect(jsonPath("$.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("CA1: registrar segunda decisión en misma evaluación retorna 409")
    void ca1_registrarSegundaDecisionEnMismaEvaluacion_retorna409() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("REJECTED")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CREDIT_DECISION_ALREADY_EXISTS"));
    }

    // ==========================================================================
    // CA2 — Decisiones válidas
    // ==========================================================================

    @Test
    @DisplayName("CA2: registrar decisión REJECTED retorna 201")
    void ca2_registrarDecisionRejected_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("REJECTED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("REJECTED"));
    }

    @Test
    @DisplayName("CA2: registrar decisión MANUAL_REVIEW retorna 201")
    void ca2_registrarDecisionManualReview_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("MANUAL_REVIEW")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("MANUAL_REVIEW"));
    }

    @Test
    @DisplayName("CA2: registrar decisión ESCALATED retorna 201")
    void ca2_registrarDecisionEscalated_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("ESCALATED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("ESCALATED"));
    }

    // ==========================================================================
    // CA3 — Observaciones mínimo 20 caracteres
    // ==========================================================================

    @Test
    @DisplayName("CA3: observaciones con menos de 20 caracteres retorna 400")
    void ca3_observacionesCortas_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "observations", "Ok"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA3: observaciones con exactamente 20 caracteres retorna 201")
    void ca3_observacionesVeinteCaracteres_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "observations", "12345678901234567890"))))
                .andExpect(status().isCreated());
    }

    // ==========================================================================
    // CA4 / RN1 — Knockout restriction
    // ==========================================================================

    @Test
    @DisplayName("CA4/RN1: evaluación knock-out con APPROVED retorna 400")
    void ca4_rn1_knockoutConApproved_retorna400() throws Exception {
        // Crear un segundo solicitante con datos que activan knock-out
        String applicantJson = objectMapper.writeValueAsString(Map.of(
                "nombre", "Juan Pérez Knockout",
                "identificacion", "1017234569",
                "fecha_nacimiento", "1990-05-15",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 3500000,
                "antiguedad_laboral", 36));

        MvcResult applicantResult = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicantJson))
                .andExpect(status().isCreated())
                .andReturn();

        String koApplicantId = objectMapper.readValue(
                applicantResult.getResponse().getContentAsString(), Map.class).get("id").toString();

        // Registrar datos financieros con muchas moras
        java.util.HashMap<String, Object> financialMapKo = new java.util.HashMap<>();
        financialMapKo.put("annualIncome", 36000000);
        financialMapKo.put("monthlyExpenses", 2000000);
        financialMapKo.put("currentDebts", 5000000);
        financialMapKo.put("assetsValue", 20000000);
        financialMapKo.put("declaredPatrimony", 15000000);
        financialMapKo.put("hasOutstandingDefaults", true);
        financialMapKo.put("creditHistoryMonths", 12);
        financialMapKo.put("defaultsLast12m", 10); // muchas moras → activa knock-out
        financialMapKo.put("defaultsLast24m", 0);
        financialMapKo.put("externalBureauScore", 720);
        financialMapKo.put("activeCreditProducts", 3);

        mockMvc.perform(post("/api/v1/solicitantes/" + koApplicantId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financialMapKo)))
                .andExpect(status().isCreated());

        // Ejecutar evaluación (debería ser knock-out)
        String evalJson = objectMapper.writeValueAsString(Map.of(
                "applicantId", koApplicantId,
                "modelId", modelId.toString()));

        MvcResult evalResult = mockMvc.perform(post("/api/v1/evaluaciones")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evalJson))
                .andExpect(status().isCreated())
                .andReturn();

        UUID koEvaluationId = UUID.fromString(objectMapper.readValue(
                evalResult.getResponse().getContentAsString(), Map.class).get("id").toString());

        mockMvc.perform(post("/api/v1/evaluaciones/" + koEvaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CREDIT_DECISION_KNOCKOUT_RESTRICTION"));
    }

    @Test
    @DisplayName("CA4/RN1: evaluación knock-out con REJECTED retorna 201")
    void ca4_rn1_knockoutConRejected_retorna201() throws Exception {
        // Similar al anterior pero con REJECTED
        String applicantJson = objectMapper.writeValueAsString(Map.of(
                "nombre", "Juan Pérez Knockout",
                "identificacion", "1017234570",
                "fecha_nacimiento", "1990-05-15",
                "tipo_empleo", "Empleado",
                "ingresos_mensuales", 3500000,
                "antiguedad_laboral", 36));

        MvcResult applicantResult = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicantJson))
                .andExpect(status().isCreated())
                .andReturn();

        String koApplicantId = objectMapper.readValue(
                applicantResult.getResponse().getContentAsString(), Map.class).get("id").toString();

        java.util.HashMap<String, Object> financialMapKo = new java.util.HashMap<>();
        financialMapKo.put("annualIncome", 36000000);
        financialMapKo.put("monthlyExpenses", 2000000);
        financialMapKo.put("currentDebts", 5000000);
        financialMapKo.put("assetsValue", 20000000);
        financialMapKo.put("declaredPatrimony", 15000000);
        financialMapKo.put("hasOutstandingDefaults", true);
        financialMapKo.put("creditHistoryMonths", 12);
        financialMapKo.put("defaultsLast12m", 10);
        financialMapKo.put("defaultsLast24m", 0);
        financialMapKo.put("externalBureauScore", 720);
        financialMapKo.put("activeCreditProducts", 3);

        mockMvc.perform(post("/api/v1/solicitantes/" + koApplicantId + "/datos-financieros")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financialMapKo)))
                .andExpect(status().isCreated());

        String evalJson = objectMapper.writeValueAsString(Map.of(
                "applicantId", koApplicantId,
                "modelId", modelId.toString()));

        MvcResult evalResult = mockMvc.perform(post("/api/v1/evaluaciones")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evalJson))
                .andExpect(status().isCreated())
                .andReturn();

        UUID koEvaluationId = UUID.fromString(objectMapper.readValue(
                evalResult.getResponse().getContentAsString(), Map.class).get("id").toString());

        mockMvc.perform(post("/api/v1/evaluaciones/" + koEvaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("REJECTED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("REJECTED"));
    }

    // ==========================================================================
    // RN3 — Authorization
    // ==========================================================================

    @Test
    @DisplayName("RN3: RISK_MANAGER puede registrar decisión")
    void rn3_riskManagerPuedeRegistrar() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RN3: ADMIN puede registrar decisión")
    void rn3_adminPuedeRegistrar() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RN3: ANALYST no puede registrar decisión (403)")
    void rn3_analistaNoPuedeRegistrar() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RN3: sin autenticación retorna 401")
    void rn3_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================================================
    // CA7 — Response incluye todos los campos
    // ==========================================================================

    @Test
    @DisplayName("CA7: response incluye todos los campos requeridos")
    void ca7_responseIncluyeTodosLosCampos() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("\"id\"");
        assertThat(json).contains("\"evaluationId\"");
        assertThat(json).contains("\"decision\"");
        assertThat(json).contains("\"observations\"");
        assertThat(json).contains("\"decidedBy\"");
        assertThat(json).contains("\"decidedAt\"");
        assertThat(json).contains("\"createdAt\"");
    }

    // ==========================================================================
    // CA1 (modificado) — hasCreditDecision en EvaluationResponse
    // ==========================================================================

    @Test
    @DisplayName("CA1: evaluación sin decisión tiene hasCreditDecision=false")
    void ca1_evaluacionSinDecision_hasCreditDecisionFalse() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/" + evaluationId)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(jsonPath("$.hasCreditDecision").value(false));
    }

    @Test
    @DisplayName("CA1: evaluación con decisión tiene hasCreditDecision=true")
    void ca1_evaluacionConDecision_hasCreditDecisionTrue() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/" + evaluationId + "/decision")
                        .with(user("analista").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestDecision("APPROVED")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/evaluaciones/" + evaluationId)
                        .with(user("analista").roles("ANALYST")))
                .andExpect(jsonPath("$.hasCreditDecision").value(true));
    }
}
