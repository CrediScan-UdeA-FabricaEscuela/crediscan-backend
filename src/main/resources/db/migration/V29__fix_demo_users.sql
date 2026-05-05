-- =============================================================================
-- V29: Fix demo user credentials and add missing demo users
-- =============================================================================

-- Fix admin password (hash for 'admin123')
UPDATE app_user
SET password_hash = '$2b$10$qbKY3twhcGwhcs4oG08o7er9g0RniQ9ox75zZTemlgNvBLPxsyLf.'
WHERE username = 'admin';

-- Add analista1 (password: 'pass1234')
INSERT INTO app_user (id, username, email, password_hash, role, enabled, password_changed_at, created_at, created_by)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'analista1',
    'analista1@creditscoring.local',
    '$2b$10$obSMt8BHvQCnliE6hqkQueNrnDugjKpRORwegbw26DR2QE0sfi6Ly',
    'ANALYST',
    true,
    NOW(),
    NOW(),
    'SYSTEM'
) ON CONFLICT (id) DO NOTHING;

-- Add riskmanager1 (password: 'pass1234')
INSERT INTO app_user (id, username, email, password_hash, role, enabled, password_changed_at, created_at, created_by)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'riskmanager1',
    'riskmanager1@creditscoring.local',
    '$2b$10$97LbkuBzFz5lElT5MgyiHOAcydt7JyMryKltiyYLvs8wti/AJZ/CO',
    'RISK_MANAGER',
    true,
    NOW(),
    NOW(),
    'SYSTEM'
) ON CONFLICT (id) DO NOTHING;
