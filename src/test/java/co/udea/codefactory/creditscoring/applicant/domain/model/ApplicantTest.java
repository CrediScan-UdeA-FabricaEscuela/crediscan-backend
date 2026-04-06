package co.udea.codefactory.creditscoring.applicant.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;

/**
 * T-04 — ApplicantTest
 * Tests the phone field addition to Applicant record.
 */
class ApplicantTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate VALID_BIRTH_DATE = LocalDate.of(1990, 5, 15);
    private static final BigDecimal VALID_INCOME = new BigDecimal("3500000");

    @Test
    void registerNew_acceptsNullPhone() {
        Applicant applicant = Applicant.registerNew(
                "Juan Pérez", "1017234567", VALID_BIRTH_DATE,
                EmploymentType.EMPLEADO, VALID_INCOME, 36, null, null, null, FIXED_CLOCK);

        assertThat(applicant.phone()).isNull();
    }

    @Test
    void registerNew_acceptsValidPhone() {
        Applicant applicant = Applicant.registerNew(
                "Juan Pérez", "1017234567", VALID_BIRTH_DATE,
                EmploymentType.EMPLEADO, VALID_INCOME, 36, "+57 310 555 1234", null, null, FIXED_CLOCK);

        assertThat(applicant.phone()).isEqualTo("+57 310 555 1234");
    }

    @Test
    void registerNew_phoneTooLong_throwsApplicantValidationException() {
        assertThatThrownBy(() -> Applicant.registerNew(
                "Juan Pérez", "1017234567", VALID_BIRTH_DATE,
                EmploymentType.EMPLEADO, VALID_INCOME, 36,
                "123456789012345678901", // 21 chars — over limit
                null, null, FIXED_CLOCK))
                .isInstanceOf(ApplicantValidationException.class)
                .hasMessageContaining("teléfono");
    }

    @Test
    void rehydrate_roundTripsPhone() {
        UUID id = UUID.randomUUID();
        Applicant applicant = Applicant.rehydrate(
                id, "Juan Pérez", "1017234567", VALID_BIRTH_DATE,
                EmploymentType.EMPLEADO, VALID_INCOME, 36, "+57 300 000 0000", null, null, FIXED_CLOCK);

        assertThat(applicant.id()).isEqualTo(id);
        assertThat(applicant.phone()).isEqualTo("+57 300 000 0000");
    }

    @Test
    void rehydrate_acceptsNullPhone() {
        UUID id = UUID.randomUUID();
        Applicant applicant = Applicant.rehydrate(
                id, "Juan Pérez", "1017234567", VALID_BIRTH_DATE,
                EmploymentType.EMPLEADO, VALID_INCOME, 36, null, null, null, FIXED_CLOCK);

        assertThat(applicant.phone()).isNull();
    }
}
