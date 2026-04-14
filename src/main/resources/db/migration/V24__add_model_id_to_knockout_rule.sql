-- =============================================================================
-- V24: Adaptar knockout_rule para HU-008 (reglas por modelo de scoring)
-- =============================================================================

-- Referencia al modelo de scoring (nullable para compatibilidad con filas existentes)
ALTER TABLE knockout_rule
    ADD COLUMN model_id UUID REFERENCES scoring_model(id) ON DELETE CASCADE;

-- Actualizar el constraint de operadores al vocabulario del dominio HU-008
ALTER TABLE knockout_rule DROP CONSTRAINT IF EXISTS knockout_rule_operator_check;
ALTER TABLE knockout_rule ADD CONSTRAINT knockout_rule_operator_check
    CHECK (operator IN ('GT', 'LT', 'GTE', 'LTE', 'EQ', 'NEQ'));

-- Índice de consulta rápida por modelo y prioridad
CREATE INDEX idx_knockout_rule_model_priority ON knockout_rule (model_id, priority)
    WHERE enabled = true;
