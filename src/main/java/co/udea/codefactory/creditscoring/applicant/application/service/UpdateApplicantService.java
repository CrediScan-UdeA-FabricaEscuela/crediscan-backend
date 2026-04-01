package co.udea.codefactory.creditscoring.applicant.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantResult;
import co.udea.codefactory.creditscoring.applicant.domain.exception.ImmutableFieldException;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.UpdateApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantEditAuditPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
public class UpdateApplicantService implements UpdateApplicantUseCase {

    private final ApplicantRepositoryPort applicantRepositoryPort;
    private final ApplicantEditAuditPort applicantEditAuditPort;
    private final Clock clock;

    @Autowired
    public UpdateApplicantService(
            ApplicantRepositoryPort applicantRepositoryPort,
            ApplicantEditAuditPort applicantEditAuditPort) {
        this(applicantRepositoryPort, applicantEditAuditPort, Clock.systemUTC());
    }

    UpdateApplicantService(
            ApplicantRepositoryPort applicantRepositoryPort,
            ApplicantEditAuditPort applicantEditAuditPort,
            Clock clock) {
        this.applicantRepositoryPort = applicantRepositoryPort;
        this.applicantEditAuditPort = applicantEditAuditPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UpdateApplicantResult update(UpdateApplicantCommand command) {
        if (command.identification() != null) {
            throw new ImmutableFieldException("identificacion",
                    "El campo 'identificacion' no puede ser modificado");
        }
        if (command.birthDate() != null) {
            throw new ImmutableFieldException("fecha_nacimiento",
                    "El campo 'fecha_nacimiento' no puede ser modificado");
        }

        Applicant existing = applicantRepositoryPort.findById(command.applicantId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante no encontrado"));

        List<String> changedFields = new ArrayList<>();
        String actor = command.actor();

        String newName = resolveString("nombre", command.name(), existing.name(), changedFields, actor, existing.id());
        EmploymentType newEmploymentType = resolveEmploymentType(command.employmentType(), existing.employmentType(), changedFields, actor, existing.id());
        BigDecimal newMonthlyIncome = resolveBigDecimal("ingresos_mensuales", command.monthlyIncome(), existing.monthlyIncome(), changedFields, actor, existing.id());
        Integer newWorkExp = resolveInteger("antiguedad_laboral", command.workExperienceMonths(), existing.workExperienceMonths(), changedFields, actor, existing.id());
        String newPhone = resolveString("telefono", command.phone(), existing.phone(), changedFields, actor, existing.id());

        Applicant updated = Applicant.rehydrate(
                existing.id(),
                newName,
                existing.identification(),
                existing.birthDate(),
                newEmploymentType,
                newMonthlyIncome,
                newWorkExp,
                newPhone,
                clock);

        Applicant saved = applicantRepositoryPort.update(updated);
        return new UpdateApplicantResult(saved, List.copyOf(changedFields));
    }

    private String resolveString(String fieldName, String newVal, String oldVal,
            List<String> changedFields, String actor, java.util.UUID applicantId) {
        if (newVal == null) return oldVal;
        if (!Objects.equals(newVal, oldVal)) {
            applicantEditAuditPort.saveEditAudit(applicantId, fieldName, oldVal, newVal, actor);
            changedFields.add(fieldName);
        }
        return newVal;
    }

    private BigDecimal resolveBigDecimal(String fieldName, BigDecimal newVal, BigDecimal oldVal,
            List<String> changedFields, String actor, java.util.UUID applicantId) {
        if (newVal == null) return oldVal;
        if (oldVal == null || newVal.compareTo(oldVal) != 0) {
            applicantEditAuditPort.saveEditAudit(applicantId, fieldName,
                    oldVal != null ? oldVal.toPlainString() : null,
                    newVal.toPlainString(), actor);
            changedFields.add(fieldName);
        }
        return newVal;
    }

    private Integer resolveInteger(String fieldName, Integer newVal, Integer oldVal,
            List<String> changedFields, String actor, java.util.UUID applicantId) {
        if (newVal == null) return oldVal;
        if (!Objects.equals(newVal, oldVal)) {
            applicantEditAuditPort.saveEditAudit(applicantId, fieldName,
                    oldVal != null ? oldVal.toString() : null,
                    newVal.toString(), actor);
            changedFields.add(fieldName);
        }
        return newVal;
    }

    private EmploymentType resolveEmploymentType(String newVal, EmploymentType oldVal,
            List<String> changedFields, String actor, java.util.UUID applicantId) {
        if (newVal == null) return oldVal;
        EmploymentType newType = EmploymentType.fromApiValue(newVal);
        if (!Objects.equals(newType, oldVal)) {
            applicantEditAuditPort.saveEditAudit(applicantId, "tipo_empleo",
                    oldVal != null ? oldVal.apiValue() : null,
                    newType.apiValue(), actor);
            changedFields.add("tipo_empleo");
        }
        return newType;
    }
}
