CREATE TABLE scoring_model (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(20)     NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'DEPRECATED')),
    min_score       NUMERIC(5,2)    NOT NULL DEFAULT 0 CHECK (min_score >= 0),
    max_score       NUMERIC(5,2)    NOT NULL DEFAULT 100 CHECK (max_score <= 1000),
    version         INTEGER         NOT NULL DEFAULT 1,
    activated_at    TIMESTAMP WITH TIME ZONE,
    deprecated_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(100)    NOT NULL,
    updated_by      VARCHAR(100)
);

CREATE UNIQUE INDEX uk_scoring_model_active ON scoring_model (status) WHERE status = 'ACTIVE';
CREATE INDEX idx_scoring_model_status ON scoring_model (status);

CREATE TABLE scoring_variable (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    description     VARCHAR(500),
    variable_type   VARCHAR(20)     NOT NULL CHECK (variable_type IN ('NUMERIC', 'CATEGORICAL')),
    source_field    VARCHAR(100)    NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(100)    NOT NULL,
    updated_by      VARCHAR(100)
);
