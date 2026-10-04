package com.uv.enzona.api.boletaje.web.dto;

import com.uv.enzona.api.boletaje.modelo.EstadoOrden;
import com.uv.enzona.api.boletaje.modelo.EstadoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Mismo contrato que {@code OrdenCreadaDto} de la app Android. */
public record OrdenRespuesta(
        Long ordenId,
        Long eventoId,
        EstadoOrden estadoOrden,
        @Schema(description = "null en confirmaciones gratuitas o si la pasarela aún no responde") EstadoPago estadoPago,
        @Schema(example = "360.00") String total,
        String moneda,
        String referenciaPago,
        @Schema(description = "Hasta cuándo se reservan los lugares si el pago sigue pendiente") OffsetDateTime expiraEn,
        String mensaje,
        List<BoletoRespuesta> boletos) {
}
