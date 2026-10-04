package com.uv.enzona.api.boletaje.web.dto;

import com.uv.enzona.api.boletaje.modelo.EstadoBoleto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Boleto con lo necesario para mostrarlo (contrato {@code BoletoDto} de la app Android más datos del evento). */
public record BoletoRespuesta(
        Long id,
        Long ordenId,
        Long eventoId,
        Long tipoBoletoId,
        Long asientoId,
        String codigo,
        String firma,
        @Schema(description = "Texto que se codifica en el QR: \"codigo.firma\"") String contenidoQr,
        EstadoBoleto estado,
        OffsetDateTime fechaEmision,
        OffsetDateTime fechaUso,
        String eventoNombre,
        OffsetDateTime fechaHoraInicio,
        String lugar,
        String tipoNombre,
        String asientoEtiqueta) {
}
