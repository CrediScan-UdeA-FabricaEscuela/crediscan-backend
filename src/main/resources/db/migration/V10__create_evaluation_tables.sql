-- =============================================================================
-- V10: Create evaluation tables (immutable - no updated_at/updated_by)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- evaluation
-- -----------------------------------------------------------------------------
CREATE TABLE evaluation (
    id              UUID                     PRIMARY KEY,
    applicant_id    UUID                     NOT NULL REFERENCES applicant(id),
    model_id        UUID                     NOT NULL REFERENCES scoring_model(id),
    financial_data_id UUID                   NOT NULL REFERENCES financial_data(id),
    total_score     NUMERIC(5,2)             NOT NULL CHECK (total_score >= 0 AND total_score <= 100),
    risk_level      VARCHAR(20)              NOT NULL CHECK (risk_level IN ('VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH')),
    knocked_out     BOOLEAN                  NOT NULL DEFAULT false,
    knockout_reasons VARCHAR(1000),
    evaluated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    evaluated_by    VARCHAR(100)             NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100)             NOT NULL
);

CREATE INDEX idx_evaluation_applicant_date ON evaluation (applicant_id, evaluated_at DESC);
CREATE INDEX idx_evaluation_risk_level     ON evaluation (risk_level);
CREATE INDEX idx_evaluation_model          ON evaluation (model_id);
CREATE INDEX idx_evaluation_evaluated_at   ON evaluation (evaluated_at);

-- -----------------------------------------------------------------------------
-- evaluation_detail
-- -----------------------------------------------------------------------------
CREATE TABLE evaluation_detail (
    id              UUID                     PRIMARY KEY,
    evaluation_id   UUID                     NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    variable_id     UUID                     NOT NULL REFERENCES scoring_variable(id),
    variable_name   VARCHAR(100)             NOT NULL,
    raw_value       VARCHAR(255)             NOT NULL,
    score           NUMERIC(5,2)             NOT NULL CHECK (score >= 0 AND score <= 100),
    weight          NUMERIC(5,4)             NOT NULL,
    weighted_score  NUMERIC(5,2)             NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_evaluation_detail_evaluation ON evaluation_detail (evaluation_id);

-- -----------------------------------------------------------------------------
-- evaluation_knockout
-- -----------------------------------------------------------------------------
CREATE TABLE evaluation_knockout (
    id              UUID                     PRIMARY KEY,
    evaluation_id   UUID                     NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    rule_id         UUID                     NOT NULL REFERENCES knockout_rule(id),
    rule_name       VARCHAR(100)             NOT NULL,
    field_value     VARCHAR(255)             NOT NULL,
    triggered       BOOLEAN                  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_evaluation_knockout_evaluation ON evaluation_knockout (evaluation_id);
CREATE INDEX idx_evaluation_knockout_triggered  ON evaluation_knockout (evaluation_id) WHERE triggered = true;
