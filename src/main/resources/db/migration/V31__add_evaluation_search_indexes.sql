-- =============================================================================
-- V31: índices para soportar búsqueda avanzada de evaluaciones (HU-014)
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_evaluation_evaluated_by
    ON evaluation (evaluated_by);

CREATE INDEX IF NOT EXISTS idx_evaluation_total_score
    ON evaluation (total_score);

-- Compuesto: optimiza el JOIN + filtro por decision en search/stats.
CREATE INDEX IF NOT EXISTS idx_credit_decision_eval_decision
    ON credit_decision (evaluation_id, decision);
