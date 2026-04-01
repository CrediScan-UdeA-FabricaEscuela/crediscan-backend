-- =============================================================================
-- V16: Replace AUDITOR role with CREDIT_SUPERVISOR
-- Order: UPDATE data first, then ALTER constraint, then seed new permissions
-- =============================================================================

-- Step 1: Migrate existing AUDITOR users to CREDIT_SUPERVISOR
UPDATE app_user
SET role = 'CREDIT_SUPERVISOR'
WHERE role = 'AUDITOR';

-- Step 2: Migrate existing AUDITOR permission entries to CREDIT_SUPERVISOR
UPDATE role_permission
SET role = 'CREDIT_SUPERVISOR'
WHERE role = 'AUDITOR';

-- Step 3: Replace CHECK constraint to remove AUDITOR and add CREDIT_SUPERVISOR
ALTER TABLE app_user
    DROP CONSTRAINT IF EXISTS app_user_role_check;

ALTER TABLE app_user
    ADD CONSTRAINT app_user_role_check
        CHECK (role IN ('ADMIN', 'ANALYST', 'RISK_MANAGER', 'CREDIT_SUPERVISOR'));

-- Step 4: Insert CREDIT_SUPERVISOR-specific permissions (evaluation CREATE/READ, plus full READ set)
INSERT INTO role_permission (id, role, resource, action, created_at)
SELECT gen_random_uuid(), 'CREDIT_SUPERVISOR', resource, action, NOW()
FROM (VALUES
    ('EVALUATION',       'CREATE'),
    ('EVALUATION',       'READ'),
    ('CREDIT_DECISION',  'READ'),
    ('APPLICANT',        'READ'),
    ('FINANCIAL_DATA',   'READ'),
    ('SCORING_MODEL',    'READ'),
    ('SCORING_VARIABLE', 'READ'),
    ('KNOCKOUT_RULE',    'READ'),
    ('REPORT',           'READ'),
    ('AUDIT_LOG',        'READ')
) AS new_perms(resource, action)
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role = 'CREDIT_SUPERVISOR'
      AND rp.resource = new_perms.resource
      AND rp.action = new_perms.action
);
