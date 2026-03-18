CREATE TABLE customers (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(200)  NOT NULL,
    email            VARCHAR(255)  NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ,
    created_by       VARCHAR(100)  NOT NULL DEFAULT 'system',
    last_modified_by VARCHAR(100)
);

CREATE INDEX idx_customers_email ON customers (email);
