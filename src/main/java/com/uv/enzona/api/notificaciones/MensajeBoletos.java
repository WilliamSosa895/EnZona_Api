package com.uv.enzona.api.notificaciones;

import java.time.OffsetDateTime;
import java.util.List;

/** Datos del correo con los boletos de una orden (RF-12). */
public record MensajeBoletos(String destinatario, String nombre, String evento, OffsetDateTime fechaHoraInicio,
        String lugar, List<Boleto> boletos) {

    public record Boleto(String codigo, String contenidoQr, String tipo, String asiento) {
    }
}
