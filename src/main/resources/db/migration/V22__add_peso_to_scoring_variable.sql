-- =============================================================================
-- V22: Agregar campo peso a scoring_variable para HU-006.
--      La validación de rango (0.01-1.00) se refuerza en la capa de aplicación;
--      a nivel DB se usa un CHECK permisivo para no bloquear valores ya existentes.
--      Se hace source_field nullable porque HU-006 no lo requiere como entrada.
-- =============================================================================

ALTER TABLE scoring_variable
    ADD COLUMN peso NUMERIC(5,4)
        CHECK (peso IS NULL OR (peso >= 0.0001 AND peso <= 1.0000));

ALTER TABLE scoring_variable
    ALTER COLUMN source_field DROP NOT NULL;
