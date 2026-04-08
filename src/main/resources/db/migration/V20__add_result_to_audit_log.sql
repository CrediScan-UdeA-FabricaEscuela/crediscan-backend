ALTER TABLE audit_log
    ADD COLUMN result VARCHAR(20);

UPDATE audit_log
    SET result = 'SUCCESS' WHERE result IS NULL;

ALTER TABLE audit_log
    ALTER COLUMN result SET NOT NULL;

ALTER TABLE audit_log
    ALTER COLUMN result SET DEFAULT 'SUCCESS';

ALTER TABLE audit_log
    ADD CONSTRAINT chk_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE'));
