-- =============================================================================
-- V23: Actualizar constraint de estado en scoring_model para HU-007.
--      La HU requiere los estados: DRAFT, ACTIVE, INACTIVE.
--      Se mantiene DEPRECATED por compatibilidad con datos previos.
-- =============================================================================

ALTER TABLE scoring_model DROP CONSTRAINT scoring_model_status_check;

ALTER TABLE scoring_model
    ADD CONSTRAINT scoring_model_status_check
    CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'DEPRECATED'));
