-- =============================================================================
-- V12: Create applicant_edit_audit table (field-level change tracking)
-- =============================================================================

CREATE TABLE applicant_edit_audit (
    id              UUID                     PRIMARY KEY,
    applicant_id    UUID                     NOT NULL REFERENCES applicant(id),
    field_name      VARCHAR(100)             NOT NULL,
    old_value       VARCHAR(500),
    new_value       VARCHAR(500),
    changed_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_by      VARCHAR(100)             NOT NULL
);

CREATE INDEX idx_applicant_edit_audit_applicant
    ON applicant_edit_audit (applicant_id, changed_at DESC);
