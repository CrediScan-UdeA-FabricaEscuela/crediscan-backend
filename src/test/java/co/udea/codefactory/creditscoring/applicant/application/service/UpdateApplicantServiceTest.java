package co.udea.codefactory.creditscoring.applicant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import co.udea.codefactory.creditscoring.applicant.domain.exception.ImmutableFieldException;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantEditAuditPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * T-16 — UpdateApplicantServiceTest
 */
@ExtendWith(MockitoExtension.class)
class UpdateApplicantServiceTest {

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
                new BigDecimal("3500000"), 36, null, FIXED_CLOCK);
    }

    private UpdateApplicantCommand commandWith(UUID id, String phone) {
        return new UpdateApplicantCommand(id, "analyst", null, null, null, null, null, null, phone);
    }

    @Test
    void update_withIdentificationInCommand_throwsImmutableFieldException() {
        UUID id = UUID.randomUUID();
        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, "9999999999", null, null, null, null, null);

        assertThatThrownBy(() -> createService().update(command))
                .isInstanceOf(ImmutableFieldException.class)
                .hasMessageContaining("identificacion");
    }

    @Test
    void update_withBirthDateInCommand_throwsImmutableFieldException() {
        UUID id = UUID.randomUUID();
        UpdateApplicantCommand command = new UpdateApplicantCommand(
                id, "analyst", null, null, LocalDate.of(1995, 1, 1), null, null, null, null);

        assertThatThrownBy(() -> createService().update(command))
                .isInstanceOf(ImmutableFieldException.class)
                .hasMessageContaining("fecha_nacimiento");
    }

    @Test
    void update_withUnknownApplicantId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.empty());
        UpdateApplicantCommand command = commandWith(id, "+57 300 000 0000");

        assertThatThrownBy(() -> createService().update(command))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withChangedPhone_recordsAuditForPhone() {
        UUID id = UUID.randomUUID();
        Applicant existing = existingApplicant(id);
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = commandWith(id, "+57 310 555 1234");
        createService().update(command);

        verify(applicantEditAuditPort).saveEditAudit(eq(id), eq("telefono"), eq(null), eq("+57 310 555 1234"), eq("analyst"));
    }

    @Test
    void update_withUnchangedField_doesNotRecordAuditForThatField() {
        UUID id = UUID.randomUUID();
        Applicant existing = existingApplicant(id);
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        // Only phone changes; name is not in command (null)
        UpdateApplicantCommand command = commandWith(id, "+57 310 000 0000");
        createService().update(command);

        verify(applicantEditAuditPort, never()).saveEditAudit(eq(id), eq("nombre"), any(), any(), any());
    }

    @Test
    void update_happyPath_returnsResultWithUpdatedApplicantAndChangedFields() {
        UUID id = UUID.randomUUID();
        Applicant existing = existingApplicant(id);
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicantCommand command = commandWith(id, "+57 310 555 9999");
        UpdateApplicantResult result = createService().update(command);

        assertThat(result.applicant()).isNotNull();
        assertThat(result.applicant().phone()).isEqualTo("+57 310 555 9999");
        assertThat(result.changedFields()).contains("telefono");
    }

    @Test
    void update_withNullCommandField_doesNotSaveAudit() {
        UUID id = UUID.randomUUID();
        Applicant existing = existingApplicant(id);
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(applicantRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

        // All fields null except applicantId — no field is being changed
        UpdateApplicantCommand command = new UpdateApplicantCommand(id, "analyst", null, null, null, null, null, null, null);
        UpdateApplicantResult result = createService().update(command);

        verify(applicantEditAuditPort, never()).saveEditAudit(any(), any(), any(), any(), any());
        assertThat(result.changedFields()).isEmpty();
    }
}
