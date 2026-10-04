package com.uv.enzona.api.validacion.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidacionSolicitud(
        @NotNull @Schema(description = "Evento en cuya puerta se valida") Long eventoId,
        @NotBlank @Size(max = 300) @Schema(description = "Contenido leído del QR: \"codigo.firma\"") String qr) {
}
