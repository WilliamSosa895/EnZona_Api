-- =====================================================================
--  EnZona — V7: recuperación de contraseña
--   · codigo_verificacion.proposito: para qué se emitió el código.
--       VERIFICACION — activar la cuenta (RF-02), el único uso hasta ahora.
--       RECUPERACION — restablecer la contraseña desde «¿Olvidaste tu
--                      contraseña?» sin iniciar sesión.
--   Los códigos de cada propósito se comprueban por separado: un código
--   de recuperación nunca activa una cuenta ni al revés.
-- =====================================================================

ALTER TABLE codigo_verificacion
    ADD COLUMN proposito VARCHAR(15) NOT NULL DEFAULT 'VERIFICACION'
        CHECK (proposito IN ('VERIFICACION', 'RECUPERACION'));

DROP INDEX IF EXISTS idx_codigo_verificacion_usuario;
CREATE INDEX idx_codigo_verificacion_usuario ON codigo_verificacion (usuario_id, proposito, canal, usado);
