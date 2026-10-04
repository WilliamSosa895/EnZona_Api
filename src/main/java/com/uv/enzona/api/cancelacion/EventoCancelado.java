package com.uv.enzona.api.cancelacion;

/** Evento de dominio: se canceló un evento. Dispara reembolsos y avisos a los asistentes. */
public record EventoCancelado(Long eventoId, String nombreEvento, String motivo) {
}
