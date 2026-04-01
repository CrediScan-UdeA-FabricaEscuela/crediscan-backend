-- =============================================================================
-- V18: Add phone column to applicant table + ANALYST APPLICANT UPDATE (HU-002)
-- =============================================================================

ALTER TABLE applicant ADD COLUMN phone VARCHAR(20) NULL;

INSERT INTO role_permission (id, role, resource, action, created_at)
VALUES (gen_random_uuid(), 'ANALYST', 'APPLICANT', 'UPDATE', NOW())
ON CONFLICT DO NOTHING;
