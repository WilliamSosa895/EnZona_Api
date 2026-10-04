package com.uv.enzona.api.cancelacion.web.dto;

import java.time.OffsetDateTime;

/** Mismo contrato que {@code CancelacionResultadoDto} de la app Android. */
public record CancelacionRespuesta(Long eventoId, String importeReembolsado, String comision, int boletosAnulados,
        int asistentesNotificados, Long canceladoPor, OffsetDateTime fechaCancelacion) {
}
