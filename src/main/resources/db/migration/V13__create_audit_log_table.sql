-- Central audit log table — partitioned by month using pg_partman.
-- pg_partman handles automatic partition creation and retention.
-- 5-year retention policy.

CREATE EXTENSION IF NOT EXISTS pg_partman;

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
) PARTITION BY RANGE (created_at);

-- Register with pg_partman for automatic monthly partitioning
SELECT partman.create_parent(
    p_parent_table := 'public.audit_log',
    p_control := 'created_at',
    p_type := 'range',
    p_interval := 'monthly',
    p_premake := 3
);

-- Retention: drop partitions older than 5 years
UPDATE partman.part_config
SET retention = '5 years',
    retention_keep_table = false
WHERE parent_table = 'public.audit_log';

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (actor);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

COMMENT ON TABLE audit_log IS '5-year retention policy. Partitioned monthly via pg_partman. Run partman.run_maintenance() periodically to create future partitions and drop expired ones.';
