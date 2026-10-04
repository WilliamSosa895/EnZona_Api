package com.uv.enzona.api.cancelacion.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Mismo contrato que {@code CancelacionDto} de la app Android. */
public record CancelacionSolicitud(
        @Schema(description = "Obligatorio si cancela un administrador (moderación)") @Size(max = 255) String motivo,
        @Schema(description = "Reservado para la comisión de cancelación (pendiente); se ignora") Boolean aceptaComision) {
}
