package com.uv.enzona.api.validacion.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Escaneo hecho sin conexión: el QR leído y la hora del dispositivo en que se escaneó. */
public record RegistroOffline(@NotBlank @Size(max = 300) String qr, @NotNull OffsetDateTime fechaHora) {
}
