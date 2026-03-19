CREATE TABLE financial_data (
    id                      UUID            PRIMARY KEY,
    applicant_id            UUID            NOT NULL REFERENCES applicant(id),
    version                 INTEGER         NOT NULL DEFAULT 1,
    annual_income           NUMERIC(19,2)   NOT NULL CHECK (annual_income >= 0),
    monthly_expenses        NUMERIC(19,2)   NOT NULL CHECK (monthly_expenses >= 0),
    current_debts           NUMERIC(19,2)   NOT NULL CHECK (current_debts >= 0),
    assets_value            NUMERIC(19,2)   NOT NULL CHECK (assets_value >= 0),
    declared_patrimony      NUMERIC(19,2)   NOT NULL CHECK (declared_patrimony >= 0),
    has_outstanding_defaults BOOLEAN        NOT NULL DEFAULT false,
    credit_history_months   INTEGER         NOT NULL CHECK (credit_history_months >= 0),
    created_at              TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE,
    created_by              VARCHAR(100)    NOT NULL,
    updated_by              VARCHAR(100),

    CONSTRAINT uk_financial_data_applicant_version UNIQUE (applicant_id, version)
);

CREATE INDEX idx_financial_data_applicant ON financial_data (applicant_id);
