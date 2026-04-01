package co.udea.codefactory.creditscoring.shared.security.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CreateUserIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM audit_log WHERE entity_type = 'USER' AND actor != 'SYSTEM'");
        jdbcTemplate.update("DELETE FROM app_user WHERE username != 'admin'");
    }

    // --- AC-01: ADMIN creates user → 201 + Location + body ---

    @Test
    void shouldCreateUserSuccessfully_returns201WithLocationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("new.user", "new@udea.co", Role.ANALYST.name())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/auth/usuarios/")));
    }

    @Test
    void shouldCreateUserSuccessfully_responseBodyContainsIdUsernameEmailRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("new.user2", "new2@udea.co", "ANALYST")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("new.user2"))
                .andExpect(jsonPath("$.email").value("new2@udea.co"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void shouldCreateUserSuccessfully_userExistsInDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("db.user", "db@udea.co", "RISK_MANAGER")))
                .andExpect(status().isCreated());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, "db.user");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldCreateUserSuccessfully_passwordIsStoredAsBCryptHash() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("hash.user", "hash@udea.co", "ANALYST")))
                .andExpect(status().isCreated());

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE username = ?", String.class, "hash.user");
        assertThat(hash).startsWith("$2");
        assertThat(hash).doesNotContain("Segura#2025");
    }

    @Test
    void shouldCreateUserSuccessfully_auditLogEntryIsRecorded() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("audit.user", "audit@udea.co", "ANALYST")))
                .andExpect(status().isCreated());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'USER' AND action = 'CREATE'",
                Integer.class);
        assertThat(auditCount).isGreaterThanOrEqualTo(1);
    }

    // --- AC-02: Non-ADMIN → 403 ---

    @Test
    void shouldReturn403WhenCallerIsAnalyst() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("valid.user", "valid@x.co", "ANALYST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenCallerIsRiskManager() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("rm").roles("RISK_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("valid.user", "valid@x.co", "ANALYST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenCallerIsCreditSupervisor() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("cs").roles("CREDIT_SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("valid.user", "valid@x.co", "ANALYST")))
                .andExpect(status().isForbidden());
    }

    // --- AC-03: Unauthenticated → 401 ---

    @Test
    void shouldReturn401WhenRequestIsUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("valid.user", "valid@x.co", "ANALYST")))
                .andExpect(status().isUnauthorized());
    }

    // --- AC-04: Duplicate username → 409 ---

    @Test
    void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("dup.user", "dup1@udea.co", "ANALYST")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("dup.user", "dup2@udea.co", "ANALYST")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409WhenUsernameAlreadyExists_errorCodeIsDuplicateUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("dup.code", "dupcode1@udea.co", "ANALYST")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("dup.code", "dupcode2@udea.co", "ANALYST")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_USER"));
    }

    // --- AC-05: Duplicate email → 409 ---

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("email.user1", "shared@udea.co", "ANALYST")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("email.user2", "shared@udea.co", "ANALYST")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists_errorCodeIsDuplicateUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("email.code1", "sharedcode@udea.co", "ANALYST")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("email.code2", "sharedcode@udea.co", "ANALYST")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_USER"));
    }

    // --- AC-06..12: Invalid payload → 400 ---

    @Test
    void shouldReturn400WhenUsernameIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.co\",\"password\":\"Segura#2025\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenEmailIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"password\":\"Segura#2025\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"email\":\"x@x.co\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRoleIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"email\":\"x@x.co\",\"password\":\"Segura#2025\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUsernameTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"email\":\"x@x.co\",\"password\":\"Segura#2025\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUsernameHasIllegalCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user name\",\"email\":\"x@x.co\",\"password\":\"Segura#2025\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"email\":\"not-an-email\",\"password\":\"Segura#2025\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"email\":\"x@x.co\",\"password\":\"short\",\"rol\":\"ANALYST\"}"))
                .andExpect(status().isBadRequest());
    }

    private String validPayload(String username, String email, String role) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "Segura#2025",
                  "rol": "%s"
                }
                """.formatted(username, email, role);
    }

    private enum Role { ADMIN, ANALYST, RISK_MANAGER, CREDIT_SUPERVISOR }
}
