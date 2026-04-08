-- V21: Make entity_id nullable in audit_log for login failures
ALTER TABLE audit_log ALTER COLUMN entity_id DROP NOT NULL;
