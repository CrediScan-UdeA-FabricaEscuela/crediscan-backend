CREATE TABLE model_variable (
    id                  UUID            PRIMARY KEY,
    model_id            UUID            NOT NULL REFERENCES scoring_model(id),
    variable_id         UUID            NOT NULL REFERENCES scoring_variable(id),
    weight              NUMERIC(5,4)    NOT NULL CHECK (weight > 0 AND weight <= 1),
    ranges_snapshot     JSONB,
    created_at          TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(100)    NOT NULL,
    updated_by          VARCHAR(100),

    CONSTRAINT uk_model_variable_model_variable UNIQUE (model_id, variable_id)
);

CREATE INDEX idx_model_variable_model ON model_variable (model_id);
CREATE INDEX idx_model_variable_variable ON model_variable (variable_id);

CREATE TABLE variable_range (
    id              UUID            PRIMARY KEY,
    variable_id     UUID            NOT NULL REFERENCES scoring_variable(id),
    min_value       NUMERIC(19,2)   NOT NULL,
    max_value       NUMERIC(19,2)   NOT NULL,
    score           INTEGER         NOT NULL CHECK (score >= 0 AND score <= 100),
    label           VARCHAR(50),
    created_at      TIMESTAMP WITH TIME ZONE     NOT NULL,

    CHECK (min_value < max_value)
);

CREATE INDEX idx_variable_range_variable ON variable_range (variable_id);

CREATE TABLE variable_category (
    id                  UUID            PRIMARY KEY,
    variable_id         UUID            NOT NULL REFERENCES scoring_variable(id),
    category_value      VARCHAR(100)    NOT NULL,
    score               INTEGER         NOT NULL CHECK (score >= 0 AND score <= 100),
    label               VARCHAR(50),
    created_at          TIMESTAMP WITH TIME ZONE     NOT NULL,

    CONSTRAINT uk_variable_category_variable_value UNIQUE (variable_id, category_value)
);

CREATE INDEX idx_variable_category_variable ON variable_category (variable_id);
