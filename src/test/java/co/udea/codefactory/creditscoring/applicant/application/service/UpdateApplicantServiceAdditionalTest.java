package co.udea.codefactory.creditscoring.applicant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantResult;
import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantEditAuditPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;

/**
 * Cubre las ramas no alcanzadas en {@link UpdateApplicantServiceTest}:
 * - cambio de tipo de empleo (resolveEmploymentType)
 * - cambio de ingresos mensuales (resolveBigDecimal — oldVal null / mismo valor)
 * - cambio de antigüedad (resolveInteger)
 * - nombre idéntico → sin auditoría
 * - tipo de empleo inválido → excepción
 */
@ExtendWith(MockitoExtension.class)
class UpdateApplicantServiceAdditionalTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    @Mock
    private ApplicantEditAuditPort applicantEditAuditPort;

    private UpdateApplicantService createService() {
        return new UpdateApplicantService(applicantRepositoryPort, applicantEditAuditPort, FIXED_CLOCK);
    }

    private Applicant existingApplicant(UUID id) {
        return Applicant.rehydrate(id, "Juan Pérez", "1017234567",
                LocalDate.of(1990, 5, 15), EmploymentType.EMPLEADO,
                new BigDecimal("3500000"), 36, null, null, null, FIXED_CLOCK);
    }

    // =========================================================================
    // Cambio de EmploymentType
    // =========================================================================

    @Test
    void update_conNuevoTipoEmpleoDistinto_registraAuditoriaYActualizaTipo() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, "Independiente", null, null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.applicant().employmentType()).isEqualTo(EmploymentType.INDEPENDIENTE);
        assertThat(result.changedFields()).contains("tipo_empleo");
        verify(applicantEditAuditPort).saveEditAudit(
                eq(id), eq("tipo_empleo"), eq("Empleado"), eq("Independiente"), eq("analyst"));
    }

    @Test
    void update_conMismoTipoEmpleo_noRegistraAuditoria() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        // El mismo tipo que ya tiene → sin cambio
        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, "Empleado", null, null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.changedFields()).doesNotContain("tipo_empleo");
    }

    @Test
    void update_conTipoEmpleoInvalido_lanzaApplicantValidationException() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));

        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, "FREELANCER", null, null, null, null, null);

        assertThatThrownBy(() -> createService().update(command))
                .isInstanceOf(ApplicantValidationException.class);
    }

    // =========================================================================
    // Cambio de ingresos mensuales (BigDecimal)
    // =========================================================================

    @Test
    void update_conNuevosIngresosMensuales_registraAuditoriaYActualiza() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal nuevoIngreso = new BigDecimal("5000000");
        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, null, nuevoIngreso, null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.applicant().monthlyIncome()).isEqualByComparingTo(nuevoIngreso);
        assertThat(result.changedFields()).contains("ingresos_mensuales");
        verify(applicantEditAuditPort).saveEditAudit(
                eq(id), eq("ingresos_mensuales"), eq("3500000"), eq("5000000"), eq("analyst"));
    }

    @Test
    void update_conMismosIngresosMensuales_noRegistraAuditoria() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        // El mismo valor → sin cambio
        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, null, new BigDecimal("3500000"), null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.changedFields()).doesNotContain("ingresos_mensuales");
    }

    // =========================================================================
    // Cambio de antigüedad laboral (Integer)
    // =========================================================================

    @Test
    void update_conNuevaAntiguedadLaboral_registraAuditoriaYActualiza() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, null, null, 48, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.applicant().workExperienceMonths()).isEqualTo(48);
        assertThat(result.changedFields()).contains("antiguedad_laboral");
        verify(applicantEditAuditPort).saveEditAudit(
                eq(id), eq("antiguedad_laboral"), eq("36"), eq("48"), eq("analyst"));
    }

    @Test
    void update_conMismaAntiguedadLaboral_noRegistraAuditoria() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        // mismo valor 36 → sin cambio
        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, null, null, null, 36, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.changedFields()).doesNotContain("antiguedad_laboral");
    }

    // =========================================================================
    // Cambio de nombre con mismo valor (String)
    // =========================================================================

    @Test
    void update_conMismoNombre_noRegistraAuditoria() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", "Juan Pérez", null, null, null, null, null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.changedFields()).doesNotContain("nombre");
    }

    @Test
    void update_conNuevoNombre_registraAuditoriaYActualiza() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", "Juan Carlos Pérez", null, null, null, null, null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.changedFields()).contains("nombre");
        assertThat(result.applicant().name()).isEqualTo("Juan Carlos Pérez");
    }

    // =========================================================================
    // Cambios múltiples simultáneos
    // =========================================================================

    @Test
    void update_conMultiplesCambosCambios_registraAuditoriaPorCadaUno() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existingApplicant(id)));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", "Nuevo Nombre", null, null, "Independiente",
                new BigDecimal("6000000"), null, null, null, null);

        UpdateApplicantResult result = createService().update(command);

        assertThat(result.changedFields()).contains("nombre", "tipo_empleo", "ingresos_mensuales");
    }
}
