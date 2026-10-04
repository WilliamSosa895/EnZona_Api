-- =====================================================================
--  EnZona — V2: identidad y acceso (incremento 1)
--   · curp_hash: índice ciego (HMAC-SHA256) del CURP. El CURP se guarda
--     cifrado con AES-GCM en la columna curp; como el cifrado usa un
--     vector aleatorio, la unicidad y la búsqueda se hacen sobre el hash.
--   · intentos_fallidos / bloqueo_hasta: bloqueo temporal de sesión.
--   · codigo_verificacion: códigos de un solo uso (OTP) para RF-02. Solo
--     se guarda el hash del código, nunca el código en claro.
-- =====================================================================

ALTER TABLE usuario
    ADD COLUMN curp_hash          VARCHAR(64),
    ADD COLUMN intentos_fallidos  SMALLINT    NOT NULL DEFAULT 0,
    ADD COLUMN bloqueo_hasta      TIMESTAMPTZ;

CREATE UNIQUE INDEX ux_usuario_curp_hash ON usuario (curp_hash) WHERE curp_hash IS NOT NULL;

CREATE TABLE codigo_verificacion (
    id              BIGSERIAL     PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    canal           VARCHAR(10)   NOT NULL CHECK (canal IN ('CORREO', 'TELEFONO')),
    codigo_hash     VARCHAR(100)  NOT NULL,
    expira_en       TIMESTAMPTZ   NOT NULL,
    intentos        SMALLINT      NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    usado           BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_creacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_codigo_verificacion_usuario ON codigo_verificacion (usuario_id, canal, usado);
