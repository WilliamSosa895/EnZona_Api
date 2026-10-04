package com.uv.enzona.api.validacion.web.dto;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Lista de boletos del evento para validar sin conexión (RNF-08, Anexo E DA-06). El dispositivo
 * compara el QR leído contra este manifiesto; la clave HMAC nunca sale del servidor.
 */
public record ManifiestoRespuesta(Long eventoId, String evento, OffsetDateTime generadoEn, int total,
        List<Entrada> boletos) {

    public record Entrada(String codigo, String firma, EstadoBoleto estado, String tipoBoleto,
            @Schema(description = "null si el evento no tiene asientos numerados") String asiento) {
    }
}
