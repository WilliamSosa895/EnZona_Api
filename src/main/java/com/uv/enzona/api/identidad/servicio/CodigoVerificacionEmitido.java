package com.uv.enzona.api.identidad.servicio;

import com.uv.enzona.api.identidad.modelo.CanalVerificacion;

/**
 * Evento de dominio: se generó un código de un solo uso. El módulo de notificaciones lo
 * envía después de confirmar la transacción. {@link #toString()} oculta el código para
 * que nunca termine en la bitácora por accidente.
 */
public record CodigoVerificacionEmitido(String destino, CanalVerificacion canal, String nombre,
        String codigo, int minutosVigencia) {

    @Override
    public String toString() {
        return "CodigoVerificacionEmitido[canal=" + canal + ", codigo=******]";
    }
}
