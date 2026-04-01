-- Align schema with DB team design (CrediScanUdeA.sql)
-- Changes:
--   1. audit_log: simplify PK from composite (id, created_at) to (id)
--   2. token_blacklist: add created_by, updated_by columns

-- -----------------------------------------------------------------------------
-- 1. audit_log — simple PK
-- -----------------------------------------------------------------------------
ALTER TABLE audit_log DROP CONSTRAINT audit_log_pkey;
ALTER TABLE audit_log ADD PRIMARY KEY (id);

-- -----------------------------------------------------------------------------
-- 2. token_blacklist — auditing columns
-- -----------------------------------------------------------------------------
ALTER TABLE token_blacklist ADD COLUMN created_by VARCHAR(100);
ALTER TABLE token_blacklist ADD COLUMN updated_by VARCHAR(100);
