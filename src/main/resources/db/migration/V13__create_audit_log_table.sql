-- Central audit log table.
-- In production, pg_partman can be added via a separate DBA runbook
-- to enable automatic monthly partitioning and 5-year retention.

CREATE TABLE audit_log (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'READ', 'LOGIN', 'LOGOUT', 'EVALUATE', 'DECIDE')),
    actor VARCHAR(100) NOT NULL,
    actor_ip VARCHAR(45),
    data_before JSONB,
    data_after JSONB,
    details VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (actor);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
