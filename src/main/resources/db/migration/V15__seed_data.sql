-- =============================================================================
-- V15: Seed data (default admin user + RBAC permission matrix)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Default admin user
-- -----------------------------------------------------------------------------
INSERT INTO app_user (id, username, email, password_hash, role, enabled, password_changed_at, created_at, created_by)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    'admin@creditscoring.local',
    '$2a$10$dXJ3SW6G7P50lGmMQoeVhOaLM0d3Rg0Xi6eP8MhiH9LGXBFBbLQiW',
    'ADMIN',
    true,
    NOW(),
    NOW(),
    'SYSTEM'
);

-- -----------------------------------------------------------------------------
-- RBAC permission matrix
-- -----------------------------------------------------------------------------

-- ADMIN: full CRUD on all resources
INSERT INTO role_permission (id, role, resource, action, created_at) VALUES
    (gen_random_uuid(), 'ADMIN', 'APPLICANT',        'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'APPLICANT',        'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'APPLICANT',        'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'APPLICANT',        'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'FINANCIAL_DATA',   'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'FINANCIAL_DATA',   'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'FINANCIAL_DATA',   'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'FINANCIAL_DATA',   'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_MODEL',    'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_MODEL',    'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_MODEL',    'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_MODEL',    'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_VARIABLE', 'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_VARIABLE', 'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_VARIABLE', 'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'SCORING_VARIABLE', 'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'KNOCKOUT_RULE',    'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'KNOCKOUT_RULE',    'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'KNOCKOUT_RULE',    'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'KNOCKOUT_RULE',    'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'EVALUATION',       'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'EVALUATION',       'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'EVALUATION',       'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'EVALUATION',       'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'CREDIT_DECISION',  'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'CREDIT_DECISION',  'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'CREDIT_DECISION',  'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'CREDIT_DECISION',  'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'USER',             'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'USER',             'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'USER',             'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'USER',             'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'AUDIT_LOG',        'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'AUDIT_LOG',        'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'AUDIT_LOG',        'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'AUDIT_LOG',        'DELETE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'REPORT',           'CREATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'REPORT',           'READ',   NOW()),
    (gen_random_uuid(), 'ADMIN', 'REPORT',           'UPDATE', NOW()),
    (gen_random_uuid(), 'ADMIN', 'REPORT',           'DELETE', NOW());

-- ANALYST: CREATE/READ on APPLICANT, FINANCIAL_DATA; READ on models/variables/rules;
--          CREATE/READ on EVALUATION; READ on CREDIT_DECISION, REPORT
INSERT INTO role_permission (id, role, resource, action, created_at) VALUES
    (gen_random_uuid(), 'ANALYST', 'APPLICANT',        'CREATE', NOW()),
    (gen_random_uuid(), 'ANALYST', 'APPLICANT',        'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'FINANCIAL_DATA',   'CREATE', NOW()),
    (gen_random_uuid(), 'ANALYST', 'FINANCIAL_DATA',   'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'SCORING_MODEL',    'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'SCORING_VARIABLE', 'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'KNOCKOUT_RULE',    'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'EVALUATION',       'CREATE', NOW()),
    (gen_random_uuid(), 'ANALYST', 'EVALUATION',       'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'CREDIT_DECISION',  'READ',   NOW()),
    (gen_random_uuid(), 'ANALYST', 'REPORT',           'READ',   NOW());

-- RISK_MANAGER: READ on APPLICANT, FINANCIAL_DATA; full CRUD on models/variables/rules;
--               READ on EVALUATION; CREATE/READ/UPDATE on CREDIT_DECISION; READ on REPORT
INSERT INTO role_permission (id, role, resource, action, created_at) VALUES
    (gen_random_uuid(), 'RISK_MANAGER', 'APPLICANT',        'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'FINANCIAL_DATA',   'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_MODEL',    'CREATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_MODEL',    'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_MODEL',    'UPDATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_MODEL',    'DELETE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_VARIABLE', 'CREATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_VARIABLE', 'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_VARIABLE', 'UPDATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'SCORING_VARIABLE', 'DELETE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'KNOCKOUT_RULE',    'CREATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'KNOCKOUT_RULE',    'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'KNOCKOUT_RULE',    'UPDATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'KNOCKOUT_RULE',    'DELETE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'EVALUATION',       'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'CREDIT_DECISION',  'CREATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'CREDIT_DECISION',  'READ',   NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'CREDIT_DECISION',  'UPDATE', NOW()),
    (gen_random_uuid(), 'RISK_MANAGER', 'REPORT',           'READ',   NOW());

-- AUDITOR: READ on all resources
INSERT INTO role_permission (id, role, resource, action, created_at) VALUES
    (gen_random_uuid(), 'AUDITOR', 'APPLICANT',        'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'FINANCIAL_DATA',   'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'SCORING_MODEL',    'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'SCORING_VARIABLE', 'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'KNOCKOUT_RULE',    'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'EVALUATION',       'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'CREDIT_DECISION',  'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'USER',             'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'AUDIT_LOG',        'READ', NOW()),
    (gen_random_uuid(), 'AUDITOR', 'REPORT',           'READ', NOW());
