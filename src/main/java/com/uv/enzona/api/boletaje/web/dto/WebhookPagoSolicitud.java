package com.uv.enzona.api.boletaje.web.dto;

import com.uv.enzona.api.boletaje.pasarela.EstadoCobro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Notificación de la pasarela sobre un cobro que quedó pendiente. */
public record WebhookPagoSolicitud(
        @NotBlank String claveIdempotencia,
        String referencia,
        @NotNull EstadoCobro estado) {
}
