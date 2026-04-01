package co.udea.codefactory.creditscoring.applicant.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * T-01/T-02 — V18MigrationTest
 * Verifies that V18 runs cleanly:
 * 1. phone column exists on applicant table
 * 2. ANALYST has APPLICANT UPDATE permission
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class V18MigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v18_phoneColumnExistsOnApplicantTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'applicant' AND column_name = 'phone'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void v18_analystHasApplicantUpdatePermission() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission " +
                "WHERE role = 'ANALYST' AND resource = 'APPLICANT' AND action = 'UPDATE'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
