package co.udea.codefactory.creditscoring.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.users.analyst.username=analyst",
        "app.security.users.analyst.password=analyst-secret",
        "app.security.crypto.encryption-key-base64=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "app.security.crypto.hash-key-base64=RkVEQ0JBOTg3NjU0MzIxMEZFRENCQTk4NzY1NDMyMTA="
})
class ApplicantRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM applicant");
    }

    @Test
    void shouldRegisterApplicantSuccessfullyAndStoreEncryptedIdentification() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Empleado", "1990-05-15", 3500000, 36)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Solicitante registrado exitosamente"))
                .andExpect(jsonPath("$.solicitante.identificacion").value("1017234567"))
                .andExpect(jsonPath("$.solicitante.antiguedad_laboral").value(36))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        UUID id = UUID.fromString(responseBody.get("id").toString());

        String encrypted = jdbcTemplate.queryForObject(
                "SELECT identification_encrypted FROM applicant WHERE id = ?",
                String.class,
                id);

        Integer months = jdbcTemplate.queryForObject(
                "SELECT work_experience_months FROM applicant WHERE id = ?",
                Integer.class,
                id);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo("1017234567");
        assertThat(months).isEqualTo(36);
    }

    @Test
    void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        String payload = """
                {
                  "identificacion": "1017234567",
                  "fecha_nacimiento": "1990-05-15",
                  "ingresos_mensuales": 3500000,
                  "tipo_empleo": "Empleado",
                  "antiguedad_laboral": 36
                }
                """;

        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Todos los campos obligatorios deben estar diligenciados"));
    }

    @Test
    void shouldReturnConflictWhenIdentificationIsDuplicated() throws Exception {
        String payload = validRequest("1017234567", "Empleado", "1990-05-15", 3500000, 36);

        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("El solicitante con esa identificación ya está registrado en el sistema"));
    }

    @Test
    void shouldReturnBadRequestWhenMonthlyIncomeIsZero() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Empleado", "1990-05-15", 0, 36)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Los ingresos mensuales deben ser un valor numérico mayor a cero"));
    }

    @Test
    void shouldReturnBadRequestWhenMonthlyIncomeIsNegative() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Empleado", "1990-05-15", -500000, 36)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Los ingresos mensuales deben ser un valor numérico mayor a cero"));
    }

    @Test
    void shouldReturnBadRequestWhenApplicantIsUnderAge() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Empleado", "2010-03-17", 3500000, 36)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Solo se aceptan solicitantes mayores de 18 años"));
    }

    @Test
    void shouldReturnBadRequestWhenEmploymentTypeIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(httpBasic("analyst", "analyst-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Freelancer", "1990-05-15", 3500000, 36)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Tipo de empleo no válido. Valores permitidos: Empleado, Independiente, Pensionado, Desempleado"));
    }

    @Test
    void shouldRejectWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Empleado", "1990-05-15", 3500000, 36)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWhenUserDoesNotHaveRole() throws Exception {
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("other-user").roles("VISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("1017234567", "Empleado", "1990-05-15", 3500000, 36)))
                .andExpect(status().isForbidden());
    }

    private String validRequest(
            String identification,
            String employmentType,
            String birthDate,
            int monthlyIncome,
            int workExperienceMonths) {
        return """
                {
                  "nombre": "Juan Carlos Perez",
                  "identificacion": "%s",
                  "fecha_nacimiento": "%s",
                  "ingresos_mensuales": %d,
                  "tipo_empleo": "%s",
                  "antiguedad_laboral": %d
                }
                """.formatted(identification, birthDate, monthlyIncome, employmentType, workExperienceMonths);
    }
}
