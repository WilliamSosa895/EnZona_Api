package com.uv.enzona.api.eventos.servicio;

/** Evento de dominio: cambió la fecha, la hora o el lugar de un evento publicado. */
public record EventoModificado(Long eventoId, String nombreEvento, String resumenCambios) {
}
