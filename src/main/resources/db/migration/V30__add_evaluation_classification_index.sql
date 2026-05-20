-- V30: índice compuesto que acelera la query de clasificacion
-- Filtra por risk_level y rango de fechas; sirve también para la query
-- con ROW_NUMBER() + WHERE evaluated_at BETWEEN ...
CREATE INDEX IF NOT EXISTS idx_evaluation_risk_level_date
    ON evaluation (risk_level, evaluated_at DESC);
