-- =============================================================================
-- V19: Add fields required by HU-001 and HU-004 that were missing in the
--      original implementation.
--
-- HU-001: applicant needs address (dirección) and email (correo electrónico).
-- HU-004: financial_data needs defaults_last_12m, defaults_last_24m,
--          external_bureau_score, and active_credit_products.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- applicant: add address and email
-- ---------------------------------------------------------------------------
ALTER TABLE applicant ADD COLUMN address VARCHAR(500) NULL;
ALTER TABLE applicant ADD COLUMN email   VARCHAR(255) NULL;

-- ---------------------------------------------------------------------------
-- financial_data: add detailed default/credit fields
-- ---------------------------------------------------------------------------
ALTER TABLE financial_data
    ADD COLUMN defaults_last_12m       INTEGER NOT NULL DEFAULT 0 CHECK (defaults_last_12m >= 0),
    ADD COLUMN defaults_last_24m       INTEGER NOT NULL DEFAULT 0 CHECK (defaults_last_24m >= 0),
    ADD COLUMN external_bureau_score   INTEGER NULL     CHECK (external_bureau_score IS NULL OR (external_bureau_score >= 0 AND external_bureau_score <= 999)),
    ADD COLUMN active_credit_products  INTEGER NOT NULL DEFAULT 0 CHECK (active_credit_products >= 0);
