package co.udea.codefactory.creditscoring.applicant.domain.port.out;

import java.util.UUID;

/**
 * Output port for recording field-level edit audit entries in the applicant_edit_audit table.
 * Specific to the applicant bounded context; distinct from the shared AuditLogPort.
 */
public interface ApplicantEditAuditPort {

    /**
     * Records a single field change audit entry.
     *
     * @param applicantId the applicant whose field changed
     * @param fieldName   the API field name (e.g., "nombre", "telefono", "ingresos_mensuales")
     * @param oldValue    the previous value as string (null for newly set fields)
     * @param newValue    the new value as string (null for cleared fields)
     * @param changedBy   the username of the actor performing the edit
     */
    void saveEditAudit(UUID applicantId, String fieldName, String oldValue, String newValue, String changedBy);
}
