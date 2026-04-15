-- Tabla para escenarios de simulación guardados (HU-009)
CREATE TABLE simulation_scenario (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id     UUID         NOT NULL REFERENCES scoring_model(id) ON DELETE CASCADE,
    name         VARCHAR(200) NOT NULL,
    description  VARCHAR(500),
    values_snapshot JSONB    NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by   VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_simulation_scenario_model ON simulation_scenario (model_id);
