-- Índices para mejorar el rendimiento de los reportes analíticos HU-016 y HU-017.
-- Soporta filtros por rango de fecha (evaluated_at) y analista (evaluated_by).
CREATE INDEX IF NOT EXISTS idx_evaluation_evaluated_by_evaluated_at
    ON evaluation (evaluated_by, evaluated_at);

-- Índice para búsqueda de decisiones por analista y fecha de decisión.
CREATE INDEX IF NOT EXISTS idx_credit_decision_decided_by_decided_at
    ON credit_decision (decided_by, decided_at);
