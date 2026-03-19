CREATE TABLE knockout_rule (
    id                  UUID            PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    description         VARCHAR(500),
    field               VARCHAR(100)    NOT NULL,
    operator            VARCHAR(20)     NOT NULL CHECK (operator IN ('EQUALS', 'NOT_EQUALS', 'GREATER_THAN', 'LESS_THAN', 'GREATER_EQUAL', 'LESS_EQUAL', 'IN', 'NOT_IN')),
    threshold_value     VARCHAR(255)    NOT NULL,
    priority            INTEGER         NOT NULL DEFAULT 0,
    enabled             BOOLEAN         NOT NULL DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(100)    NOT NULL,
    updated_by          VARCHAR(100)
);

CREATE INDEX idx_knockout_rule_enabled ON knockout_rule (enabled, priority) WHERE enabled = true;
