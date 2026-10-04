-- =====================================================================
--  EnZona — V4: boletaje y pagos (incrementos 3 a 5)
--   · Compra múltiple: orden_detalle guarda el tipo, la cantidad, el precio
--     confirmado y los asientos de la orden (mismo diseño que la migración
--     v2 de la app Android).
--   · Idempotencia: clave_operacion única por asistente (Idempotency-Key).
--   · Reserva con expiración: expira_en libera lugares y asientos de las
--     órdenes cuyo pago no se confirmó.
--   · Un asiento no puede tener dos boletos vigentes (red de seguridad).
--   · Reembolsos: referencia y fecha del reembolso en pago.
-- =====================================================================

ALTER TABLE orden
    ADD COLUMN clave_operacion VARCHAR(64),
    ADD COLUMN expira_en       TIMESTAMPTZ;

CREATE UNIQUE INDEX ux_orden_clave_operacion
    ON orden (asistente_id, clave_operacion)
    WHERE clave_operacion IS NOT NULL;

CREATE INDEX idx_orden_evento    ON orden (evento_id);
CREATE INDEX idx_orden_pendiente ON orden (estado, expira_en);

CREATE TABLE orden_detalle (
    id               BIGSERIAL     PRIMARY KEY,
    orden_id         BIGINT        NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
    tipo_boleto_id   BIGINT        NOT NULL REFERENCES tipo_boleto(id) ON DELETE RESTRICT,
    cantidad         INTEGER       NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    asientos_ids     BIGINT[]      NOT NULL DEFAULT '{}',
    CONSTRAINT ck_orden_detalle_asientos
        CHECK (cardinality(asientos_ids) = 0 OR cardinality(asientos_ids) = cantidad)
);

CREATE INDEX idx_orden_detalle_orden ON orden_detalle (orden_id);

CREATE UNIQUE INDEX ux_boleto_asiento_vigente
    ON boleto (asiento_id)
    WHERE asiento_id IS NOT NULL AND estado IN ('VALIDO', 'USADO');

CREATE INDEX idx_boleto_orden ON boleto (orden_id);
CREATE INDEX idx_pago_orden   ON pago (orden_id);

ALTER TABLE pago
    ADD COLUMN referencia_reembolso VARCHAR(120),
    ADD COLUMN fecha_reembolso      TIMESTAMPTZ;
