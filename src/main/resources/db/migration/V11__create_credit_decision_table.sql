-- =============================================================================
-- V11: Create credit_decision table (1:1 with evaluation)
-- =============================================================================

CREATE TABLE credit_decision (
    id              UUID                     PRIMARY KEY,
    evaluation_id   UUID                     NOT NULL UNIQUE REFERENCES evaluation(id),
    decision        VARCHAR(20)              NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED', 'MANUAL_REVIEW')),
    observations    VARCHAR(2000)            NOT NULL CHECK (LENGTH(observations) >= 20),
    approved_amount NUMERIC(19,2)            CHECK (approved_amount >= 0),
    interest_rate   NUMERIC(5,2)             CHECK (interest_rate >= 0 AND interest_rate <= 100),
    term_months     INTEGER                  CHECK (term_months > 0),
    decided_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_by      VARCHAR(100)             NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100)             NOT NULL
);

CREATE INDEX idx_credit_decision_evaluation ON credit_decision (evaluation_id);
CREATE INDEX idx_credit_decision_decision   ON credit_decision (decision);
CREATE INDEX idx_credit_decision_decided_at ON credit_decision (decided_at);
