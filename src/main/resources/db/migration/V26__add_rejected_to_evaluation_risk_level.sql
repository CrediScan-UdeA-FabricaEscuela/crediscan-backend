-- =============================================================================
-- V26: Agregar 'REJECTED' al CHECK constraint de evaluation.risk_level
-- Necesario para evaluaciones que resultaron rechazadas por regla knockout
-- =============================================================================

ALTER TABLE evaluation DROP CONSTRAINT IF EXISTS evaluation_risk_level_check;
ALTER TABLE evaluation ADD CONSTRAINT evaluation_risk_level_check
    CHECK (risk_level IN ('VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH', 'REJECTED'));
