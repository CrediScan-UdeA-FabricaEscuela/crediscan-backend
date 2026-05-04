-- =============================================================================
-- V28: Add supervisor_id and resolution_deadline_at for ESCALATED decisions (RN4, CA7)
-- =============================================================================

ALTER TABLE credit_decision
    ADD COLUMN supervisor_id         VARCHAR(100),
    ADD COLUMN resolution_deadline_at TIMESTAMP WITH TIME ZONE;
