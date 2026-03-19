-- =============================================================================
-- V14: Stored procedures and views
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Function: recalculate_risk_distribution
-- Returns risk level distribution with statistics for a given date range.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION recalculate_risk_distribution(
    p_from_date TIMESTAMPTZ DEFAULT NULL,
    p_to_date TIMESTAMPTZ DEFAULT NULL
)
RETURNS TABLE (
    risk_level VARCHAR,
    evaluation_count BIGINT,
    percentage NUMERIC(5,2),
    avg_score NUMERIC(5,2),
    min_score NUMERIC(5,2),
    max_score NUMERIC(5,2)
) AS $$
    WITH filtered AS (
        SELECT e.risk_level, e.total_score
        FROM evaluation e
        WHERE (p_from_date IS NULL OR e.evaluated_at >= p_from_date)
          AND (p_to_date IS NULL OR e.evaluated_at < p_to_date)
    ),
    total AS (
        SELECT COUNT(*) AS cnt FROM filtered
    )
    SELECT
        f.risk_level::VARCHAR,
        COUNT(*)::BIGINT AS evaluation_count,
        CASE WHEN t.cnt > 0 THEN ROUND(COUNT(*)::NUMERIC / t.cnt * 100, 2) ELSE 0 END AS percentage,
        ROUND(AVG(f.total_score), 2) AS avg_score,
        MIN(f.total_score) AS min_score,
        MAX(f.total_score) AS max_score
    FROM filtered f, total t
    GROUP BY f.risk_level, t.cnt
    ORDER BY evaluation_count DESC;
$$ LANGUAGE SQL STABLE;

-- -----------------------------------------------------------------------------
-- Function: cleanup_expired_tokens
-- Purges expired entries from token_blacklist. Returns count of deleted rows.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM token_blacklist WHERE expires_at < NOW();
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- View: vw_financial_data_with_ratios
-- Enriches financial_data with computed financial ratios.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_financial_data_with_ratios AS
SELECT
    fd.id,
    fd.applicant_id,
    fd.version,
    fd.annual_income,
    fd.monthly_expenses,
    fd.current_debts,
    fd.assets_value,
    fd.declared_patrimony,
    fd.has_outstanding_defaults,
    fd.credit_history_months,
    fd.created_at,
    fd.updated_at,
    fd.created_by,
    fd.updated_by,
    ROUND(fd.current_debts / NULLIF(fd.annual_income, 0), 4)           AS debt_to_income_ratio,
    ROUND((fd.monthly_expenses * 12) / NULLIF(fd.annual_income, 0), 4) AS expenses_to_income_ratio,
    fd.declared_patrimony - fd.current_debts                            AS net_patrimony
FROM financial_data fd;

-- -----------------------------------------------------------------------------
-- View: vw_evaluation_summary
-- Consolidated evaluation view with applicant, model, and decision data.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_evaluation_summary AS
SELECT
    e.id                    AS evaluation_id,
    a.name                  AS applicant_name,
    a.identification_hash   AS applicant_identification_hash,
    sm.name                 AS model_name,
    e.total_score,
    e.risk_level,
    e.knocked_out,
    e.evaluated_at,
    cd.decision,
    cd.observations,
    cd.approved_amount,
    cd.decided_at
FROM evaluation e
    INNER JOIN applicant a       ON a.id  = e.applicant_id
    INNER JOIN scoring_model sm  ON sm.id = e.model_id
    LEFT  JOIN credit_decision cd ON cd.evaluation_id = e.id;
