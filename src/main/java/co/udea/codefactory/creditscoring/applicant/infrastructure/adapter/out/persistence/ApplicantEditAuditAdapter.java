package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantEditAuditPort;

@Component
public class ApplicantEditAuditAdapter implements ApplicantEditAuditPort {

    private final JpaApplicantEditAuditRepository auditRepository;

    public ApplicantEditAuditAdapter(JpaApplicantEditAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void saveEditAudit(UUID applicantId, String fieldName, String oldValue, String newValue, String changedBy) {
        ApplicantEditAuditJpaEntity entity = new ApplicantEditAuditJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicantId(applicantId);
        entity.setFieldName(fieldName);
        entity.setOldValue(oldValue);
        entity.setNewValue(newValue);
        entity.setChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setChangedBy(changedBy);
        auditRepository.save(entity);
    }
}
