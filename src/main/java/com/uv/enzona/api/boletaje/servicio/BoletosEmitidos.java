package com.uv.enzona.api.boletaje.servicio;

/** Evento de dominio: una orden emitió sus boletos. El módulo de notificaciones envía el correo (RF-12). */
public record BoletosEmitidos(Long ordenId) {
}
