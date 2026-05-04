-- =============================================================================
-- V27: Add ESCALATED to credit_decision decision check constraint
-- =============================================================================

ALTER TABLE credit_decision DROP CONSTRAINT credit_decision_decision_check;
ALTER TABLE credit_decision ADD CONSTRAINT credit_decision_decision_check
    CHECK (decision IN ('APPROVED', 'REJECTED', 'MANUAL_REVIEW', 'ESCALATED'));
