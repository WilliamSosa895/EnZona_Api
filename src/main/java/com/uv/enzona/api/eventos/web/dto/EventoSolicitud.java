package com.uv.enzona.api.eventos.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Alta y modificación de un evento (RF-08, RF-13). */
public record EventoSolicitud(
        @NotBlank @Size(max = 160) String nombre,
        @Size(max = 5000) String descripcion,
        @NotBlank @Size(max = 160) String lugar,
        @Size(max = 240) String direccion,
        @Schema(description = "Por defecto, Orizaba (área de estudio)") @Size(max = 80) String ciudad,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitud,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitud,
        Integer categoriaId,
        @Schema(example = "2026-11-20T19:00:00-06:00") @NotNull @Future OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        @NotNull @Min(1) @Max(100000) Integer aforo,
        boolean esDePago,
        boolean requiereAsiento,
        @Schema(description = "Obligatorio en eventos de pago; no se envía en gratuitos") @Valid
        List<TipoBoletoSolicitud> tiposBoleto,
        @Schema(description = "Obligatorio si requiereAsiento: el total de asientos debe ser igual al aforo") @Valid
        List<SeccionSolicitud> secciones,
        @Schema(description = "true para publicarlo de inmediato; false lo deja en borrador")
        boolean publicar) {

    public List<TipoBoletoSolicitud> tiposONada() {
        return tiposBoleto == null ? List.of() : tiposBoleto;
    }

    public List<SeccionSolicitud> seccionesONada() {
        return secciones == null ? List.of() : secciones;
    }
}
