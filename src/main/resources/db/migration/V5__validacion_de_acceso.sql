-- =====================================================================
--  EnZona — V5: validación de acceso (incrementos 3 y 5)
--   · motivo: causa del rechazo (boleto ya usado, cancelado, de otro
--     evento, firma inválida…) para que el organizador revise la bitácora,
--     incluidos los conflictos de la validación sin conexión (CP-43).
-- =====================================================================

ALTER TABLE validacion_acceso ADD COLUMN motivo VARCHAR(30);

CREATE INDEX idx_validacion_boleto ON validacion_acceso (boleto_id);
CREATE INDEX idx_boleto_codigo     ON boleto (codigo);
