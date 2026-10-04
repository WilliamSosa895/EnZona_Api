-- =====================================================================
--  EnZona — V3: gestión de eventos (incrementos 2 y 3)
--   · Coordenadas en pares: latitud y longitud ambas o ninguna (nunca 0).
--   · Registro de la cancelación (RF-14): fecha, quién y motivo.
--   · evento_validador: personal de acceso designado por el organizador
--     para cada evento (CU-06).
-- =====================================================================

ALTER TABLE evento
    ADD CONSTRAINT ck_evento_coordenadas_pares CHECK ((latitud IS NULL) = (longitud IS NULL)),
    ADD COLUMN fecha_cancelacion   TIMESTAMPTZ,
    ADD COLUMN cancelado_por       BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    ADD COLUMN motivo_cancelacion  VARCHAR(255);

ALTER TABLE evento
    ADD CONSTRAINT ck_evento_cancelacion_completa
        CHECK (estado <> 'CANCELADO' OR fecha_cancelacion IS NOT NULL);

CREATE INDEX idx_evento_organizador ON evento (organizador_id);
CREATE INDEX idx_tipo_boleto_evento ON tipo_boleto (evento_id);
CREATE INDEX idx_asiento_evento     ON asiento (evento_id, estado);

CREATE TABLE evento_validador (
    evento_id   BIGINT NOT NULL REFERENCES evento(id)  ON DELETE CASCADE,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    PRIMARY KEY (evento_id, usuario_id)
);
