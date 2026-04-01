package co.udea.codefactory.creditscoring.shared.security.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Task 7.5 — V16MigrationTest
 * Verifies that:
 * 1. V16 runs cleanly (context loads = Flyway succeeded)
 * 2. No AUDITOR rows remain in app_user or role_permission
 * 3. CREDIT_SUPERVISOR rows exist in role_permission
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class V16MigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v16_noAuditorRowsRemainInAppUser() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE role = 'AUDITOR'",
                Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void v16_noAuditorRowsRemainInRolePermission() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission WHERE role = 'AUDITOR'",
                Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void v16_creditSupervisorPermissionsExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission WHERE role = 'CREDIT_SUPERVISOR'",
                Integer.class);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void v16_creditSupervisorHasEvaluationCreatePermission() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission WHERE role = 'CREDIT_SUPERVISOR' AND resource = 'EVALUATION' AND action = 'CREATE'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
